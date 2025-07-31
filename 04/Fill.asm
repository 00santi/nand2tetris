// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, 
// the screen should be cleared.


(LOOP)
    @SCREEN 
    D=A
    @i
    M=D

    @KBD
    D=M
    @FILL_SCREEN
    D;JNE 
    @CLEAR_SCREEN
    0;JMP

(FILL_SCREEN)
    @i
    A=M
    M=-1

    @i
    M=M+1
    
    @24576
    D=A
    @i
    D=M-D
    @LOOP
    D;JEQ

    @FILL_SCREEN
    0;JMP

(CLEAR_SCREEN)
    @i
    A=M
    M=0

    @i
    M=M+1

    @24576
    D=A
    @i
    D=M-D
    @LOOP
    D;JEQ

    @CLEAR_SCREEN
    0;JMP
