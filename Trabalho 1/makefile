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
JAVAC ?= javac
JAVA ?= java
JAVAC_FLAGS = -encoding UTF-8

all: scanner parser

scanner:
	$(JAVAC) $(JAVAC_FLAGS) -cp "$(CP)" util/*.java scanner/model/*.java scanner/persistence/*.java scanner/pipeline/*.java scanner/*.java

parser:
	$(JAVAC) $(JAVAC_FLAGS) -cp "$(CP)" util/*.java scanner/model/*.java scanner/persistence/*.java scanner/*.java parser/model/*.java parser/grammar/*.java parser/*.java

run: all
	$(JAVA) -cp "$(CP)" scanner.Main
	$(JAVA) -cp "$(CP)" parser.ParserMain

run-scanner:
	$(JAVA) -cp "$(CP)" scanner.Main

run-parser:
	$(JAVA) -cp "$(CP)" parser.ParserMain

run-scanner-file:
	$(JAVA) -cp "$(CP)" scanner.Scanner "casos_teste/teste_simples.txt"
	$(JAVA) -cp "$(CP)" scanner.Scanner "casos_teste/teste_erro.txt"
	$(JAVA) -cp "$(CP)" scanner.Scanner "casos_teste/teste_fibonacci.txt"
clean:
	$(RM) $(call FIXPATH,util/*.class)
	$(RM) $(call FIXPATH,scanner/model/*.class)
	$(RM) $(call FIXPATH,scanner/persistence/*.class)
	$(RM) $(call FIXPATH,scanner/pipeline/*.class)
	$(RM) $(call FIXPATH,scanner/*.class)
	$(RM) $(call FIXPATH,parser/model/*.class)
	$(RM) $(call FIXPATH,parser/grammar/*.class)
	$(RM) $(call FIXPATH,parser/*.class)
