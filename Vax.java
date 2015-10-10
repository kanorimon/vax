import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Vax {
	public static void main(String[] args){
		Memory.loadToMemory(args[0]);
		Disassembler.disassemble();
	}
}

class Memory{
	static byte[] memory;
	
	static void loadToMemory(String fileName){
		File file = new File(fileName);
		Path pathName = file.toPath();
		try {
			memory = java.nio.file.Files.readAllBytes(pathName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static int getSize(){
		return memory.length;
	}
	
	static byte getMemoryByte(int address){
		return memory[address];
	}
}

class Assembly {
	static int pc;
	static int printPc;
	static ArrayList<Byte> memory;
	static String opecode;
	static int opecodeMode;				//0:byte 1:word 2:long word
	static ArrayList<String> operand;
	
	static void start(){
		pc = 0;
		printPc = 0;
		memory = new ArrayList<Byte>();
		opecode = "";
		opecodeMode = 0;
		operand = new ArrayList<String>();
	}
	
	static void reset(){
		printPc = pc;
		memory.clear();
		opecode = "";
		opecodeMode = 0;
		operand.clear();
	}
	
	static void print(){
		System.out.printf("%4x:   ", printPc);
		int i=0;
		if(memory.size() > 4){
			for(;i<4;i++) System.out.printf("%02x ", memory.get(i));
		}else{
			for(;i<memory.size();i++) System.out.printf("%02x ", memory.get(i));
			for(;i<4;i++) System.out.printf("   ");
		}
		
		System.out.printf("    %s ", opecode);
		for(int j=0;j<operand.size();j++){
			System.out.printf("%s", operand.get(j));
			if(j<operand.size()-1) System.out.print(",");
		}
		System.out.print("\n");
		
		if(memory.size() > 4){
			System.out.printf("%4x:   ", printPc + 4);
			i=4;
			for(;i<memory.size();i++) System.out.printf("%02x ", memory.get(i));
			System.out.print("\n");
		}
	}
}

class Register{
	static final String[] REGISTER_NAME = {"r0","r1","r2","r3","r4","r5","r6","r7","r8","r9","","","ap","fp","sp","pc"};
}
class Disassembler {
	
	static void disassemble(){
		
		Assembly.start();
		
		while(Assembly.pc < Memory.getSize()){
			Assembly.reset();
			
			setOperand();
			
			Assembly.print();
		}
	}
	
	static byte getMemoryByte(){
		Assembly.memory.add(Memory.getMemoryByte(Assembly.pc));
		Assembly.pc++;
		return Assembly.memory.get(Assembly.memory.size()-1);
	}
	
	static void setOperand(){
		byte code = getMemoryByte();
		
		switch((code >> 4) & 0xF){
		case 0:
			switch(code & 0xF){
			case 0:
				Assembly.opecode = "halt";
				break;
			case 1:
				Assembly.opecode = "nop";
				break;
			case 4:
				Assembly.opecode = "ret";
				break;
			}
			break;
		case 9:
			switch(code & 0xF){
			case 0xe:
				Assembly.opecode = "movab";
				Assembly.opecodeMode = 0;
				for(int i=0;i<2;i++) setOpecode();
				break;
			}
			break;
		case 0xc:
			switch(code & 0xF){
			case 1:
				Assembly.opecode = "addl3";
				Assembly.opecodeMode = 2;
				for(int i=0;i<3;i++) setOpecode();
				break;
			case 2:
				Assembly.opecode = "subl2";
				Assembly.opecodeMode = 2;
				for(int i=0;i<2;i++) setOpecode();
				break;
			}
			break;
		case 0xd:
			switch(code & 0xF){
			case 0:
				Assembly.opecode = "movl";
				Assembly.opecodeMode = 2;
				for(int i=0;i<2;i++) setOpecode();
				break;
			case 0xd:
				Assembly.opecode = "pushl";
				Assembly.opecodeMode = 2;
				for(int i=0;i<1;i++) setOpecode();
				break;
			}
			break;
		case 0xf:
			switch(code & 0xF){
			case 0xb:
				Assembly.opecode = "calls";
				for(int i=0;i<2;i++) setOpecode();
				break;
			}
			break;
		}
	}
	
	static void setOpecode(){
		byte code = getMemoryByte();
		int tmp = 0;
		
		switch(code & 0xF){
		case 0xf:
			switch((code >> 4) & 0xF){
			case 8:
				/* immediate */
				switch(Assembly.opecodeMode){
				case 0:
					tmp = getMemoryByte() & 0xFF;
					break;
				case 1:
					tmp = getMemoryByte() & 0xFF;
					tmp = (tmp + (getMemoryByte() << 8)) & 0xFF00;
					break;
				case 2:
					tmp = getMemoryByte() & 0xFF;
					tmp = (tmp + (getMemoryByte() << 8)) & 0xFFFF;
					tmp = (tmp + (getMemoryByte() << 16)) & 0xFFFFFF;
					tmp = tmp + (getMemoryByte() << 24);
					break;
				}
				Assembly.operand.add(String.format("$0x%x", tmp));
				break;
			case 0xe:
				/* long word displacement */
				tmp = getMemoryByte() & 0xFF;
				tmp = (tmp + (getMemoryByte() << 8)) & 0xFFFF;
				tmp = (tmp + (getMemoryByte() << 16)) & 0xFFFFFF;
				tmp = tmp + (getMemoryByte() << 24);
				Assembly.operand.add(String.format("0x%x", Assembly.pc + tmp));
				break;
			}
			break;
		default:
			switch((code >> 4) & 0xF){
			case 0:
			case 1:
			case 2:
			case 3:
				/* literal */
				Assembly.operand.add(String.format("$0x%x", code << 24 >> 24));
				break;
			case 5:
				/* register */
				Assembly.operand.add(Register.REGISTER_NAME[code & 0xF]);
				break;
			case 6:
				/* register deferred */
				Assembly.operand.add(String.format("(%s)",Register.REGISTER_NAME[code & 0xF]));
				break;
			case 7:
				/* auto decrement */
				Assembly.operand.add(String.format("-(%s)",Register.REGISTER_NAME[code & 0xF]));
				break;
			case 8:
				/* auto increment */
				Assembly.operand.add(String.format("(%s)+",Register.REGISTER_NAME[code & 0xF]));
				break;
			case 9:
				/* auto increment deferred */
				Assembly.operand.add(String.format("*(%s)+",Register.REGISTER_NAME[code & 0xF]));
				break;
			case 0xa:
				/* byte displacement */
				tmp = getMemoryByte();
				Assembly.operand.add(String.format("0x%x(%s)", tmp << 24 >> 24, Register.REGISTER_NAME[code & 0xF]));
				break;
			case 0xb:
				/* byte displacement deferred */
				tmp = getMemoryByte();
				Assembly.operand.add(String.format("*0x%x(%s)", tmp << 24 >> 24, Register.REGISTER_NAME[code & 0xF]));
				break;			
			}
			break;
		}
	}
}