.PHONY: all scanner parser run run-scanner run-parser run-scanner-file clean

ifeq ($(OS),Windows_NT)
    SEP = ;
    RM = cmd /C del /Q /S
    FIXPATH = $(subst /,\,$(1))
else
    SEP = :
    RM = rm -f
    FIXPATH = $(1)
endif

JAR_DIR = lib
CP = .$(SEP)$(JAR_DIR)/jackson-databind-2.15.2.jar$(SEP)$(JAR_DIR)/jackson-core-2.15.2.jar$(SEP)$(JAR_DIR)/jackson-annotations-2.15.2.jar

all: scanner parser

scanner:
	javac -cp "$(CP)" scanner/model/*.java scanner/persistence/*.java scanner/pipeline/*.java scanner/*.java

parser:
	javac -cp "$(CP)" parser/model/*.java parser/grammar/*.java parser/*.java

run: all
	java -cp "$(CP)" scanner.Main
	java -cp "$(CP)" parser.ParserMain

run-scanner:
	java -cp "$(CP)" scanner.Main

run-parser:
	java -cp "$(CP)" parser.ParserMain

run-scanner-file:
	java -cp "$(CP)" scanner.Scanner "casos_teste/teste_simples.txt"
	java -cp "$(CP)" scanner.Scanner "casos_teste/teste_erro.txt"
	java -cp "$(CP)" scanner.Scanner "casos_teste/teste_fibonacci.txt"
clean:
	$(RM) $(call FIXPATH,scanner/model/*.class)
	$(RM) $(call FIXPATH,scanner/persistence/*.class)
	$(RM) $(call FIXPATH,scanner/pipeline/*.class)
	$(RM) $(call FIXPATH,scanner/*.class)
	$(RM) $(call FIXPATH,parser/model/*.class)
	$(RM) $(call FIXPATH,parser/grammar/*.class)
	$(RM) $(call FIXPATH,parser/*.class)
