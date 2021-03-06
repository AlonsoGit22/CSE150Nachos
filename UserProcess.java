package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see    nachos.vm.VMProcess
 * @see    nachos.network.NetProcess
 */
public class UserProcess {
   /**
    * Allocate a new process.
    */

   public UserProcess() {
      ProcessID = proNum++;
      status = -1;
      currentProcess.put(ProcessID, this);
      childProcesses = new HashSet<Integer>();
      finished = new Semaphore(0);
      descriptorManager = new OpenFile[16];
      descriptorManager[0] = UserKernel.console.openForReading();
      descriptorManager[1] = UserKernel.console.openForWriting();

      int numPhysPages = Machine.processor().getNumPhysPages();
      pageTable = new TranslationEntry[numPhysPages];
      for (int i=0; i<numPhysPages; i++)
         pageTable[i] = new TranslationEntry(i,i, true,false,false,false);



      //descriptorManager[0] = UserKernel.console.openForReading(); //STDIN
      //descriptorManager[1] = UserKernel.console.openForWriting(); //STDOUT
   }

   /**
    * Allocate and return a new process of the correct class. The class name
    * is specified by the <tt>nachos.conf</tt> key
    * <tt>Kernel.processClassName</tt>.
    *
    * @return a new process of the correct class.
    */
   public static UserProcess newUserProcess() {
      return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
   }

   /**
    * Execute the specified program with the specified arguments. Attempts to
    * load the program, and then forks a thread to run it.
    *
    * @param  name   the name of the file containing the executable.
    * @param  args   the arguments to pass to the executable.
    * @return <tt>true</tt> if the program was successfully executed.
    */
   public boolean execute(String name, String[] args) {
      if (!load(name, args))
         return false;

      new UThread(this).setName(name).fork();

      return true;
   }

   /**
    * Save the state of this process in preparation for a context switch.
    * Called by <tt>UThread.saveState()</tt>.
    */
   public void saveState() {
   }

   /**
    * Restore the state of this process after a context switch. Called by
    * <tt>UThread.restoreState()</tt>.
    */
   public void restoreState() {
      Machine.processor().setPageTable(pageTable);
   }

   /**
    * Read a null-terminated string from this process's virtual memory. Read
    * at most <tt>maxLength + 1</tt> bytes from the specified address, search
    * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
    * without including the null terminator. If no null terminator is found,
    * returns <tt>null</tt>.
    *
    * @param  vaddr  the starting virtual address of the null-terminated
    *       string.
    * @param  maxLength  the maximum number of characters in the string,
    *          not including the null terminator.
    * @return the string read, or <tt>null</tt> if no null terminator was
    *    found.
    */
   public String readVirtualMemoryString(int vaddr, int maxLength) {
      Lib.assertTrue(maxLength >= 0);

      byte[] bytes = new byte[maxLength+1];

      int bytesRead = readVirtualMemory(vaddr, bytes);

      for (int length=0; length<bytesRead; length++) {
         if (bytes[length] == 0)
            return new String(bytes, 0, length);
      }

      return null;
   }

   /**
    * Transfer data from this process's virtual memory to all of the specified
    * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
    *
    * @param  vaddr  the first byte of virtual memory to read.
    * @param  data   the array where the data will be stored.
    * @return the number of bytes successfully transferred.
    */
   public int readVirtualMemory(int vaddr, byte[] data) {
      return readVirtualMemory(vaddr, data, 0, data.length);
   }

   /**
    * Transfer data from this process's virtual memory to the specified array.
    * This method handles address translation details. This method must
    * <i>not</i> destroy the current process if an error occurs, but instead
    * should return the number of bytes successfully copied (or zero if no
    * data could be copied).
    *
    * @param  vaddr  the first byte of virtual memory to read.
    * @param  data   the array where the data will be stored.
    * @param  offset the first byte to write in the array.
    * @param  length the number of bytes to transfer from virtual memory to
    *       the array.
    * @return the number of bytes successfully transferred.
    */
   public int readVirtualMemory(int vaddr, byte[] data, int offset,
                         int length) {
      Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

      byte[] memory = Machine.processor().getMemory();

      // for now, just assume that virtual addresses equal physical addresses
      if (vaddr < 0 || vaddr >= memory.length)
         return 0;

      int amount = Math.min(length, memory.length-vaddr);
      System.arraycopy(memory, vaddr, data, offset, amount);

      return amount;
   }

   /**
    * Transfer all data from the specified array to this process's virtual
    * memory.
    * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
    *
    * @param  vaddr  the first byte of virtual memory to write.
    * @param  data   the array containing the data to transfer.
    * @return the number of bytes successfully transferred.
    */
   public int writeVirtualMemory(int vaddr, byte[] data) {
      return writeVirtualMemory(vaddr, data, 0, data.length);
   }

   /**
    * Transfer data from the specified array to this process's virtual memory.
    * This method handles address translation details. This method must
    * <i>not</i> destroy the current process if an error occurs, but instead
    * should return the number of bytes successfully copied (or zero if no
    * data could be copied).
    *
    * @param  vaddr  the first byte of virtual memory to write.
    * @param  data   the array containing the data to transfer.
    * @param  offset the first byte to transfer from the array.
    * @param  length the number of bytes to transfer from the array to
    *       virtual memory.
    * @return the number of bytes successfully transferred.
    */
   public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                          int length) {
      Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

      byte[] memory = Machine.processor().getMemory();

      // for now, just assume that virtual addresses equal physical addresses
      if (vaddr < 0 || vaddr >= memory.length)
         return 0;

      int amount = Math.min(length, memory.length-vaddr);
      System.arraycopy(data, offset, memory, vaddr, amount);

      return amount;
   }

   /**
    * Load the executable with the specified name into this process, and
    * prepare to pass it the specified arguments. Opens the executable, reads
    * its header information, and copies sections and arguments into this
    * process's virtual memory.
    *
    * @param  name   the name of the file containing the executable.
    * @param  args   the arguments to pass to the executable.
    * @return <tt>true</tt> if the executable was successfully loaded.
    */
   private boolean load(String name, String[] args) {
      Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

      //this is false because it is not creating the file
      OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
      if (executable == null) {
         Lib.debug(dbgProcess, "\topen failed");
         return false;
      }

      try {
         coff = new Coff(executable);
      }
      catch (EOFException e) {
         executable.close();
         Lib.debug(dbgProcess, "\tcoff load failed");
         return false;
      }

      // make sure the sections are contiguous and start at page 0
      numPages = 0;
      for (int s=0; s<coff.getNumSections(); s++) {
         CoffSection section = coff.getSection(s);
         if (section.getFirstVPN() != numPages) {
            coff.close();
            Lib.debug(dbgProcess, "\tfragmented executable");
            return false;
         }
         numPages += section.getLength();
      }

      // make sure the argv array will fit in one page
      byte[][] argv = new byte[args.length][];
      int argsSize = 0;
      for (int i=0; i<args.length; i++) {
         argv[i] = args[i].getBytes();
         // 4 bytes for argv[] pointer; then string plus one for null byte
         argsSize += 4 + argv[i].length + 1;
      }
      if (argsSize > pageSize) {
         coff.close();
         Lib.debug(dbgProcess, "\targuments too long");
         return false;
      }

      // program counter initially points at the program entry point
      initialPC = coff.getEntryPoint();

      // next comes the stack; stack pointer initially points to top of it
      numPages += stackPages;
      initialSP = numPages*pageSize;

      // and finally reserve 1 page for arguments
      numPages++;

      if (!loadSections())
         return false;

      // store arguments in last page
      int entryOffset = (numPages-1)*pageSize;
      int stringOffset = entryOffset + args.length*4;

      this.argc = args.length;
      this.argv = entryOffset;

      for (int i=0; i<argv.length; i++) {
         byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
         Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
         entryOffset += 4;
         Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
               argv[i].length);
         stringOffset += argv[i].length;
         Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
         stringOffset += 1;
      }

      return true;
   }

   /**
    * Allocates memory for this process, and loads the COFF sections into
    * memory. If this returns successfully, the process will definitely be
    * run (this is the last step in process initialization that can fail).
    *
    * @return <tt>true</tt> if the sections were successfully loaded.
    */
   protected boolean loadSections() {
      if (numPages > Machine.processor().getNumPhysPages()) {
         coff.close();
         Lib.debug(dbgProcess, "\tinsufficient physical memory");
         return false;
      }

      // load sections
      for (int s=0; s<coff.getNumSections(); s++) {
         CoffSection section = coff.getSection(s);

         Lib.debug(dbgProcess, "\tinitializing " + section.getName()
               + " section (" + section.getLength() + " pages)");

         for (int i=0; i<section.getLength(); i++) {
            int vpn = section.getFirstVPN()+i;

            // for now, just assume virtual addresses=physical addresses
            section.loadPage(i, vpn);
         }
      }

      return true;
   }

   /**
    * Release any resources allocated by <tt>loadSections()</tt>.
    */
   protected void unloadSections() {
   }

   /**
    * Initialize the processor's registers in preparation for running the
    * program loaded into this process. Set the PC register to point at the
    * start function, set the stack pointer register to point at the top of
    * the stack, set the A0 and A1 registers to argc and argv, respectively,
    * and initialize all other registers to 0.
    */
   public void initRegisters() {
      Processor processor = Machine.processor();

      // by default, everything's 0
      for (int i=0; i<processor.numUserRegisters; i++)
         processor.writeRegister(i, 0);

      // initialize PC and SP according
      processor.writeRegister(Processor.regPC, initialPC);
      processor.writeRegister(Processor.regSP, initialSP);

      // initialize the first two argument registers to argc and argv
      processor.writeRegister(Processor.regA0, argc);
      processor.writeRegister(Processor.regA1, argv);
   }

   /**
    * Handle the halt() system call.
    */
   private int handleHalt() {

      Machine.halt();

      Lib.assertNotReached("Machine.halt() did not halt machine!");
      return 0;
   }

   private int handleCreat(int vaddr){
      String filename;
      OpenFile currentFile;


      filename = this.readVirtualMemoryString(vaddr, maxFileName);
      //ThreadedKernel.fileSystem.open(java.lang.string, boolean) boolean: True when creating and opening. False when just opening
      currentFile = ThreadedKernel.fileSystem.open(filename, true);

      if(filename == null){
         //Lib.debug(dbgProcess, "File name cannot be null. Please enter a name");
         return -1;
      }

      if(currentFile == null){
         //lib.dug(dbgProcess, "File could not be created");
         return -1;
      }
      else{
        for (int i=0; i < maxFileDescriptor; i++){
          if(descriptorManager[i] == null){
            descriptorManager[i] = file;
            return i;
          }
        }
      }
             
      //lib.debug(dbgProcess, "Descriptors are full");
      return -1; //use -1 to indicate the descriptors are full

    }

  private int handleOpen(int vaddr){
    String filename;
    OpenFile currentFile;


    filename = this.readVirtualMemoryString(vaddr, maxFileName);
    //ThreadedKernel.fileSystem.open(java.lang.string, boolean) boolean: True when creating and opening. False when just opening
    currentFile = ThreadedKernel.fileSystem.open(filename, false);

    if(filename == null){
       //Lib.debug(dbgProcess, "File name cannot be null. Please enter a name");
       return -1;
     }

    if(currentFile == null){
        //lib.dug(dbgProcess, "File could not be created");
        return -1;
    }
    else{
      for (int i=0; i < maxFileDescriptor; i++){
        if(descriptorManager[i] == null){
            descriptorManager[i] = file;
            return i;
        }
      }
     }
      
   }


   private int handleRead(int handle, int buffer, int size){
      //OpenFile file;
      FileDescriptor file;
      byte[] readStream;
      int readBytes;

      currentFD = descriptorManager[handle];
      readStream = new byte[size];
      readBytes = file.currentFile.read(readStream, 0, size);

      if(handle < 0 || handle > 15 ){
         return -1;
      }
      else if(size <= 0){
         return -1;
      }
      else if(descriptorManager[handle].currentFile == null){
        return -1;
      }

      //Reading the array
      else if (readBytes < 0){
         return -1;
      }
      else{
         int writeBytes = writeVirtualMemory(handle,readStream,0,readBytes);

         if (writeBytes < 0){
            return -1;
         }
         else{
            return writeBytes;
         }
      }

   }


    private int handleWrite(int handle, int buffer, int size) {
        if (handle < 0 || handle > 15) {
            Lib.debug(dbgProcess, "Handle not in range, cannot proceed...");
            return -1;
        } else {
            if (size < 0) {
                Lib.debug(dbgProcess, "Negative file size, cannot procceed...");
                return -1;
            } else if (size == 0) {
                Lib.debug(dbgProcess, "File size is zero.");
                return 0;
            }
        }

        if (descriptorManager[handle] == null) {
            Lib.debug(dbgProcess, "File is NULL, cannot proceed...");
            return -1;
        }

        byte[] writer = new byte[size];
        int length = readVirtualMemory(buffer, writer, 0, size);
        int count = descriptorManager[handle].write(writer, 0, length);
        if (count == -1) {
            Lib.debug(dbgProcess, "Error, cannot proceed...");
            return -1;
        } else return count;
    }

    private int handleClose(int handle) {
        if (handle < 0 || handle > 15 || descriptorManager[handle] == null) {
            Lib.debug(dbgProcess, "Handle not in range or not valid, cannot proceed...");
            return -1;
        } else {
            descriptorManager[handle].close();
            descriptorManager[handle] = null;
        }
        return 0;
    }

    private int handleUnlink(int vaddr) {
        String fileName = readVirtualMemoryString(vaddr,maxFileName);
        if (fileName == null || vaddr < 0) {
            Lib.debug(dbgProcess, "Virtual address or file name not valid, cannot proceed...");
            return -1;
        }

        for(int i = 0; i <= 15; i++) {
            if (descriptorManager[i] != null && descriptorManager[i].getName().equals(fileName)){
                descriptorManager[i] = null;
                break;
            }
        }
        boolean fileRemoved = ThreadedKernel.fileSystem.remove(fileName);
        if (!fileRemoved) {
            Lib.debug(dbgProcess, "Unable to unlink file...");
            return -1;
        }
        return 0;
    }




   private static final int
         syscallHalt = 0,
         syscallExit = 1,
         syscallExec = 2,
         syscallJoin = 3,
         syscallCreate = 4,
         syscallOpen = 5,
         syscallRead = 6,
         syscallWrite = 7,
         syscallClose = 8,
         syscallUnlink = 9;

   /**
    * Handle a syscall exception. Called by <tt>handleException()</tt>. The
    * <i>syscall</i> argument identifies which syscall the user executed:
    *
    * <table>
    * <tr><td>syscall#</td><td>syscall prototype</td></tr>
    * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
    * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
    * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
    *                          </tt></td></tr>
    * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
    * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
    * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
    * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
    *                      </tt></td></tr>
    * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
    *                      </tt></td></tr>
    * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
    * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
    * </table>
    *
    * @param  syscall    the syscall number.
    * @param  a0 the first syscall argument.
    * @param  a1 the second syscall argument.
    * @param  a2 the third syscall argument.
    * @param  a3 the fourth syscall argument.
    * @return the value to be returned to the user.
    */
   public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
      switch (syscall) {
         case syscallHalt:
            return handleHalt();

         case syscallCreate:
            return handleCreat(a0);//needs to open file when creating

         case syscallOpen:
            return handleOpen(a0);

         case syscallRead:
            return handleRead(a0, a1, a2);

    case syscallWrite:
        return handleWrite(a0, a1, a2);

    case syscallClose:
        return handleClose(a0);

    case syscallUnlink:
        return handleUnlink(a0);


         default:
            Lib.debug(dbgProcess, "Unknown syscall " + syscall);
            Lib.assertNotReached("Unknown system call!");
      }
      return 0;
   }

   protected int handleJoin(int processID, int status) {
      ////check to see if processID refers to a child process of the current process if not exit
      if(!childProcesses.contains(processID)){
         Lib.debug(dbgProcess, "processID does not refer to a child process");
         return -1;
      }
      childProcesses.remove(processID);

      UserProcess child = currentProcess.get(processID);
      //handles the case wehre we join a exited process
      if(child == null) {
         Lib.debug(dgbProcess, "join a process that has exited");
         child = deadProcess.get(processID);
         if(child == null){
            Lib.debug(dbgProcess, "error in join");
            return -1;
         }
      }
      child.finished.P();
      // Here we will write in vm
      writeVirtualMemory(status, Lib.bytesFromInt(child.status));
      if(child.naturalExit)
         return 1;
      else
         return 0;
   }

   protected int handleExit(int status) {
      this.status = status;
      //starting at index 2 of hash
      for(int i = 2; i < maxFileDescriptor; i++){
         if (descriptorManager[i] != null){
            OpenFile currentFile = descriptorManager[i];
            descriptorManager[i] = null;
            currentFile.close();
         }
      }
      //making sure to free up the memory
      unloadSections();

      currentProcess.remove(ProcessID);

      deadProcess.put(ProcessID, this);

      finished.V();
      if(currentProcess.isEmpty())
         Kernel.kernel.terminate();
      //here is we call the thread to a finish
      UThread.finish();
      return 0;

   }

   /**
    * Handle a user exception. Called by
    * <tt>UserKernel.exceptionHandler()</tt>. The
    * <i>cause</i> argument identifies which exception occurred; see the
    * <tt>Processor.exceptionZZZ</tt> constants.
    *
    * @param  cause  the user exception that occurred.
    */
   public void handleException(int cause) {
      Processor processor = Machine.processor();

      switch (cause) {
         case Processor.exceptionSyscall:
            int result = handleSyscall(processor.readRegister(Processor.regV0),
                  processor.readRegister(Processor.regA0),
                  processor.readRegister(Processor.regA1),
                  processor.readRegister(Processor.regA2),
                  processor.readRegister(Processor.regA3)
            );
            processor.writeRegister(Processor.regV0, result);
            processor.advancePC();
            break;

         default:
            Lib.debug(dbgProcess, "Unexpected exception: " +
                  Processor.exceptionNames[cause]);
            Lib.assertNotReached("Unexpected exception");
      }
   }

   public class FileDescriptor {
      public OpenFile descriptorManager[] = new OpenFile[maxFileDescriptor];

      public int add(int index, OpenFile currentFile) {
         if (index >= maxFileDescriptor || index < 0)
            return -1;
         if (descriptorManager[index] == null) {
            descriptorManager[index] = currentFile;
            if (files.get(currentFile.getName()) != null) {
               files.put(currentFile.getName(), files.get(currentFile.getName()) + 1);
            } else {
               files.put(currentFile.getName(), 1);
            }
            return index;
         }
         return -1;
      }

      public int add(OpenFile currentFile) {
         for (int i = 0; i < maxFileDescriptor; i++)
            if (descriptorManager[i] == null)
               return add(i, currentFile);

         return -1;
      }

      public int close(int fileDescriptor) {
         if (descriptorManager[fileDescriptor] == null) {
            Lib.debug(dbgProcess, "The File descriptor " + fileDescriptor + " does not exist");
            return -1;
         }
         OpenFile currentFile = descriptorManager[fileDescriptor];
         descriptorManager[fileDescriptor] = null;
         file.close();

         String fileName = currentFile.getName();

         if (files.get(fileName) > 1)
            files.put(fileName, files.get(fileName) - 1);
         else {
            files.remove(fileName);
            if (removed.contains(fileName)) {
               removed.remove(fileName);
               UserKernel.fileSystem.remove(fileName);
            }
         }
         return 0;
      }

      public OpenFile get(int fileDescriptor) {
         if (fileDescriptor >= maxFileDescriptor || fileDescriptor < 0)
            return null;
         return descriptorManager[fileDescriptor];
      }
   }/*
      public FileDescriptor(){
         //will handle a single FileDescriptor
      }
      private String filename = ""; //
      private OpenFile file = null; //
      private boolean remove = false;
   }
   private FileDescriptor descriptorManager[]  = new FileDescriptor[maxFileDescriptor];
   public static final int maxFileDescriptor = 16;
   public static final int maxFileName = 256;
   private int findEmptyFD(){
      for(int i = 0; i < maxFileDescriptor; i++){
         if (descriptorManager[i].file == null){
            return i;
         }
      }
      //lib.debug(dbgProcess, "Descriptors are full");
      return -1; //use -1 to indicate the descriptors are full
   }
   private int searchFD(String fileName){
      for(int i = 0; i < maxFileDescriptor; i++){
         if (descriptorManager[i].fileName.equals(fileName){
            return i;
         }
      }
      return -1;//this indicates that there is no file found under that name
   }
*/

   /** The program being run by this process. */
   protected Coff coff;

   /** This process's page table. */
   protected TranslationEntry[] pageTable;
   /** The number of contiguous pages occupied by the program. */
   protected int numPages;

   /** The number of pages in the program's stack. */
   protected final int stackPages = 8;

   private int initialPC, initialSP;
   private int argc, argv;

   private static final int pageSize = Processor.pageSize;
   private static final char dbgProcess = 'a';

   protected OpenFile[] descriptorManager;

   protected int ProcessID;
   protected int status;
   protected Semaphore finished;
   protected HashSet<Integer> childProcesses;
   protected  boolean naturalExit = true;
   protected static int proNum = 0;
   protected static final int maxFileName = 256;
   protected static final int maxFileDescriptor = 16;
   protected static Hashtable<String, Integer> files = new Hashtable<String, Integer>();
   protected static HashSet<String> removed = new HashSet<String>();
   protected static Hashtable<Integer, UserProcess> currentProcess = new Hashtable<Integer, UserProcess>();
   protected static Hashtable<Integer, UserProcess> deadProcess = new Hashtable<Integer, UserProcess>();

}
