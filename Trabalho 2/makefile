.PHONY: all compile run run-validos run-erros run-file clean
.SILENT:

ifeq ($(OS),Windows_NT)
    FIXPATH = $(subst /,\,$(1))
    MKDIR_P = cmd /C if not exist "$(call FIXPATH,$(1))" mkdir "$(call FIXPATH,$(1))"
    RM_RF = cmd /C if exist "$(call FIXPATH,$(1))" rmdir /S /Q "$(call FIXPATH,$(1))"
    OK_IF_FAIL = || cmd /C exit 0
    PY_CMD ?= py -3
    PRINT_TITLE = cmd /C echo. && cmd /C echo $(SEPARATOR) && cmd /C echo Executando agora: $(1) && cmd /C echo $(SEPARATOR)
else
    FIXPATH = $(1)
    MKDIR_P = mkdir -p "$(call FIXPATH,$(1))"
    RM_RF = rm -rf "$(call FIXPATH,$(1))"
    OK_IF_FAIL = || true
    PY_CMD ?= python3
    PRINT_TITLE = printf '\n%s\nExecutando agora: %s\n%s\n' '$(SEPARATOR)' '$(1)' '$(SEPARATOR)'
endif

JAVAC ?= javac
JAVA ?= java
JAVAC_FLAGS = -encoding UTF-8
SEPARATOR = ----------------------------------------------------------------------

SRC_DIR = src/compiler
BUILD_DIR = build
OUT_DIR = out
MAIN_CLASS = compiler.Main
SOURCES = $(wildcard $(SRC_DIR)/*.java)

VALIDO = exemplos/programa_valido.scm
FATORIAL = exemplos/fatorial.scm
ERROS = exemplos/erros.scm
FILE ?= $(VALIDO)
OUT ?= $(OUT_DIR)/saida.py

all: compile

compile:
	$(call MKDIR_P,$(BUILD_DIR))
	$(JAVAC) $(JAVAC_FLAGS) -d $(BUILD_DIR) $(SOURCES)

run: run-validos run-erros

run-validos: compile
	$(call MKDIR_P,$(OUT_DIR))
	$(call PRINT_TITLE,compilador lendo $(VALIDO))
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) $(VALIDO) $(OUT_DIR)/programa_valido.py
	$(call PRINT_TITLE,Python executando $(OUT_DIR)/programa_valido.py)
	$(PY_CMD) $(OUT_DIR)/programa_valido.py
	$(call PRINT_TITLE,compilador lendo $(FATORIAL))
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) $(FATORIAL) $(OUT_DIR)/fatorial.py
	$(call PRINT_TITLE,Python executando $(OUT_DIR)/fatorial.py)
	$(PY_CMD) $(OUT_DIR)/fatorial.py

run-erros: compile
	$(call MKDIR_P,$(OUT_DIR))
	$(call PRINT_TITLE,compilador lendo $(ERROS))
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) $(ERROS) $(OUT_DIR)/erros.py $(OK_IF_FAIL)

run-file: compile
	$(call MKDIR_P,$(OUT_DIR))
	$(call PRINT_TITLE,compilador lendo $(FILE))
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) "$(FILE)" "$(OUT)"

clean:
	$(call RM_RF,$(BUILD_DIR))
	$(call RM_RF,$(OUT_DIR))
