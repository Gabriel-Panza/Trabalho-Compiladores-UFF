.PHONY: all run clean examples

ifeq ($(OS),Windows_NT)
    SEP = ;
    RM = cmd /C del /Q /S
    MKDIR = cmd /C mkdir
    FIXPATH = $(subst /,\,$(1))
else
    SEP = :
    RM = rm -rf
    MKDIR = mkdir -p
    FIXPATH = $(1)
endif

JAVAC ?= javac
JAVA ?= java
SRC_DIR = src
BUILD_DIR = build/classes
MAIN = compiler.Main

SOURCES = $(wildcard $(SRC_DIR)/compiler/*.java)

all:
	$(MKDIR) $(call FIXPATH,$(BUILD_DIR))
	$(JAVAC) -encoding UTF-8 -d $(BUILD_DIR) $(SOURCES)

run: all
	$(JAVA) -cp $(BUILD_DIR) $(MAIN) exemplos/programa_valido.scm out/programa_valido.py

examples: all
	$(JAVA) -cp $(BUILD_DIR) $(MAIN) exemplos/programa_valido.scm out/programa_valido.py
	$(JAVA) -cp $(BUILD_DIR) $(MAIN) exemplos/fatorial.scm out/fatorial.py

clean:
	$(RM) $(call FIXPATH,build)
	$(RM) $(call FIXPATH,out)
