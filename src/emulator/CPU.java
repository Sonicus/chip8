package emulator;


import javafx.scene.input.KeyCode;
import util.CpuUtil;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import java.util.stream.IntStream;

public class CPU {
    private long cycleNumber;
    private short PC;
    private final Stack<Short> STACK;
    private short I;
    private byte DT = 0x0;
    private byte ST = 0x0;
    private final double timerRefreshRate = 1000000000.0 / 60;
    private long timerTickTime = 0;
    private final byte[] reg = new byte[16];
    private final byte[] mem = new byte[4096];
    private int[][] vMem = new int[64][32];
    private boolean drawFlag;
    private final Random random = new Random();
    private HashMap<KeyCode, Integer> buttonMap;
    private boolean[] buttonStatus;
    private boolean waitingForKey;
    private byte waitingForKeyReg;

    public CPU(byte[] romData) {
        System.arraycopy(romData, 0, mem, 0x200, romData.length);
        initializeFont();
        PC = 0x200;
        I = 0;
        STACK = new Stack<>();
        cycleNumber = 0;
        drawFlag = false;
        initializeButtons();
    }

    private void initializeButtons() {
        buttonMap = new HashMap<>();
        buttonMap.put(KeyCode.DIGIT1, 0x1);
        buttonMap.put(KeyCode.DIGIT2, 0x2);
        buttonMap.put(KeyCode.DIGIT3, 0x3);
        buttonMap.put(KeyCode.DIGIT4, 0xC);
        buttonMap.put(KeyCode.Q, 0x4);
        buttonMap.put(KeyCode.W, 0x5);
        buttonMap.put(KeyCode.E, 0x6);
        buttonMap.put(KeyCode.R, 0xD);
        buttonMap.put(KeyCode.A, 0x7);
        buttonMap.put(KeyCode.S, 0x8);
        buttonMap.put(KeyCode.D, 0x9);
        buttonMap.put(KeyCode.F, 0xE);
        buttonMap.put(KeyCode.Z, 0xA);
        buttonMap.put(KeyCode.X, 0x0);
        buttonMap.put(KeyCode.C, 0xB);
        buttonMap.put(KeyCode.V, 0xF);
        buttonStatus = new boolean[buttonMap.size()];
        waitingForKey = false;
        waitingForKeyReg = 0x0;
    }

    void executeCycle() throws RuntimeException {
        cycleNumber++;
        if (System.nanoTime() > timerTickTime + timerRefreshRate) {
            DT = (byte) (Math.max(0, --DT));
            ST = (byte) (Math.max(0, --ST));
            timerTickTime = System.nanoTime();
        }
        if (waitingForKey) {
            return;
        }
        if (PC + 1 >= mem.length) {
            throw new RuntimeException("PC outside memory range");
        }
        short opcodeShort = CpuUtil.shortFromBytes(mem[PC], mem[PC + 1]);
        System.out.println("Current opcode is " + String.format("%04X", opcodeShort) + " (cycle " + cycleNumber + ")");
        byte[] opcodeNibbles = CpuUtil.nibblesFromShort(opcodeShort);

        switch (opcodeNibbles[0]) {
            case 0x0:
                switch (CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3])) {
                    case (byte) 0xE0:
                        CLS();
                        break;
                    case (byte) 0xEE:
                        RET();
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            case 0x1:
                JMP(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x2:
                CALL(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x3:
                SE_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x4:
                SNE_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x6:
                LD_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x7:
                ADD_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x8:
                switch (opcodeNibbles[3]) {
                    case 0x0:
                        LD_R(opcodeNibbles[1], opcodeNibbles[2]);
                        break;
                    case 0x2:
                        AND(opcodeNibbles[1], opcodeNibbles[2]);
                        break;
                    case 0x4:
                        ADD(opcodeNibbles[1], opcodeNibbles[2]);
                        break;
                    case 0x5:
                        SUB_R(opcodeNibbles[1], opcodeNibbles[2]);
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            case 0x9:
                SNE_R(opcodeNibbles[1], opcodeNibbles[2]);
                break;
            case 0xA:
                LDI(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0xC:
                RND(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0xD:
                DRW(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]);
                break;
            case 0xE:
                switch (CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3])) {
                    case (byte) 0x9E:
                        SKP(opcodeNibbles[1]);
                        break;
                    case (byte) 0xA1:
                        SKNP(opcodeNibbles[1]);
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            case 0xF:
                switch (CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3])) {
                    case 0x07:
                        LDRDT(opcodeNibbles[1]);
                        break;
                    case 0x0A:
                        LDK(opcodeNibbles[1]);
                        break;
                    case 0x15:
                        LDDT(opcodeNibbles[1]);
                        break;
                    case 0x18:
                        LDST(opcodeNibbles[1]);
                        break;
                    case 0x1E:
                        ADD_I(opcodeNibbles[1]);
                        break;
                    case 0x29:
                        LDF(opcodeNibbles[1]);
                        break;
                    case 0x33:
                        LDB(opcodeNibbles[1]);
                        break;
                    case 0x65:
                        LDR(opcodeNibbles[1]);
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            default:
                unknownOpcode(opcodeShort);
        }

        PC += 2;
    }

    private void unknownOpcode(short opcodeShort) {
        throw new RuntimeException("Unknown opcode " + String.format("%04X", opcodeShort));
    }

    //00E0
    private void CLS() {
        vMem = new int[64][32];
    }

    //00EE
    private void RET() {
        if (STACK.empty()) {
            throw new RuntimeException("Stack underflow");
        }
        PC = STACK.pop();
        PC -= 2;
        if (PC < 0) {
            throw new RuntimeException("PC is negative");
        }
    }

    //1NNN
    private void JMP(short address) {
        PC = address;
        PC -= 2; //Since the cycle execution adds +2
    }

    //2NNN
    private void CALL(short address) {
        STACK.push((short) (PC + 2));
        PC = address;
        PC -= 2;
    }

    //3XNN
    private void SE_N(byte sourceRegister, byte value) {
        if (reg[sourceRegister] == value) {
            PC += 2;
        }
    }

    //4XNN
    private void SNE_N(byte sourceRegister, byte value) {
        if (reg[sourceRegister] != value) {
            PC += 2;
        }
    }

    //6XNN
    private void LD_N(byte targetRegister, byte value) {
        reg[targetRegister] = value;
    }

    //7XNN
    private void ADD_N(byte targetRegister, byte value) {
        reg[targetRegister] = (byte) ((reg[targetRegister] + value) % 256);
    }

    //8XY0
    private void LD_R(byte targetRegister, byte sourceRegister) {
        reg[targetRegister] = reg[sourceRegister];
    }

    //8XY2
    private void AND(byte regX, byte regY) {
        reg[regX] &= reg[regY];
    }

    //8XY4
    private void ADD(byte regX, byte regY) {
        reg[0xF] = (byte) (reg[regX] + reg[regY] > 255 ? 0x01 : 0x00);
        reg[regX] = (byte) (reg[regX] + reg[regY] % 255);
    }

    //8XY5
    private void SUB_R(byte regX, byte regY) {
        reg[0xF] = (byte) (reg[regX] < reg[regY] ? 0x00 : 0x01);
        reg[regX] -= reg[regY];
    }

    //9XY0
    private void SNE_R(byte regX, byte regY) {
        if (mem[regX] != mem[regY]) {
            PC += 2;
        }
    }

    //ANNN
    private void LDI(short address) {
        I = address;
    }

    //CXNN
    private void RND(byte targetRegister, byte value) {
        reg[targetRegister] = (byte) random.nextInt(value + 1);
    }

    //DXYN
    private void DRW(byte xSource, byte ySource, byte n) {
        byte x = reg[xSource];
        byte y = reg[ySource];
        byte collisionFlag = 0x00;
        for (int j = 0; j < n; j++) {
            int screenYPos = (j + y) % 32;
            while (screenYPos < 0) {
                screenYPos += 32;
            }
            for (int i = 0; i < 8; i++) {
                int screenXPos = (x + i) % 64;
                while (screenXPos < 0) {
                    screenXPos += 64;
                }
                if (vMem[screenXPos][screenYPos] == 1 && ((mem[I + j] >> (7 - i)) & 0b00000001) == 1) {
                    collisionFlag = 0x01;
                }
                vMem[screenXPos][screenYPos] = vMem[screenXPos][screenYPos] ^ (mem[I + j] >> (7 - i) & 1);
            }
        }
        reg[0xF] = collisionFlag;
        drawFlag = true;
    }

    //EXA1
    private void SKNP(byte keyButton) {
        if (!buttonStatus[reg[keyButton]]) {
            PC += 2;
        }
    }

    //EX9E
    private void SKP(byte keyButton) {
        if (buttonStatus[reg[keyButton]]) {
            PC += 2;
        }
    }

    //FX07
    private void LDRDT(byte targetRegister) {
        reg[targetRegister] = DT;
    }

    //FX0A
    private void LDK(byte targetRegister) {
        waitingForKey = true;
        waitingForKeyReg = targetRegister;
    }

    //FX15
    private void LDDT(byte sourceRegister) {
        DT = reg[sourceRegister];
    }

    //FX18
    private void LDST(byte sourceRegister) {
        ST = reg[sourceRegister];
    }

    //FX1E
    private void ADD_I(byte regX) {
        I += reg[regX];
    }

    //FX29
    private void LDF(byte sourceRegister) {
        I = (short) (reg[sourceRegister] * 5);
    }

    //FX33
    private void LDB(byte sourceRegister) {
        if (I + 2 >= mem.length) {
            segfault();
        }
        mem[I] = (byte) (reg[sourceRegister] / 100);
        mem[I + 1] = (byte) ((reg[sourceRegister] % 100) / 10);
        mem[I + 2] = (byte) (reg[sourceRegister] % 10);
    }

    //FX65
    private void LDR(byte targetRegister) {
        if (I + targetRegister >= mem.length) {
            segfault();
        }
        IntStream.rangeClosed(0, targetRegister).forEach(index -> reg[index] = mem[I + index]);
        I = (short) (I + targetRegister + 1);
    }

    private void segfault() {
        throw new RuntimeException("Segmentation fault! I: " + String.format("%04X", I) + " Memory length: " + mem.length);
    }

    private void initializeFont() {
        byte[] font = {
                (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, //0
                (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, //1
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, //2
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //3
                (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, //4
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //5
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, //6
                (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, //7
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, //8
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //9
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, //A
                (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, //B
                (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, //C
                (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, //D
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, //E
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  //F
        };
        System.arraycopy(font, 0, mem, 0, font.length);
    }

    public long getCycleNumber() {
        return cycleNumber;
    }

    public short getPC() {
        return PC;
    }

    public byte getST() {
        return ST;
    }

    public int[][] getvMem() {
        return vMem;
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }

    public void setDrawFlag(boolean value) {
        this.drawFlag = value;
    }

    public void keyPressed(KeyCode keyCode) {
        if (buttonMap.containsKey(keyCode)) {
            buttonStatus[buttonMap.get(keyCode)] = true;
            if (waitingForKey) {
                reg[waitingForKeyReg] = buttonMap.get(keyCode).byteValue();
                waitingForKey = false;
            }
        }
    }

    public void keyReleased(KeyCode keyCode) {
        if (buttonMap.containsKey(keyCode)) {
            buttonStatus[buttonMap.get(keyCode)] = false;
        }
    }
}
