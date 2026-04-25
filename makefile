ifeq ($(OS),Windows_NT)
    # Configurações para Windows Nativo
    SEP = ;
    RM = del /Q /S
    FIXPATH = $(subst /,\,$(1))
else
    # Configurações para Linux, Mac e WSL
    SEP = :
    RM = rm -f
    FIXPATH = $(1)
endif

JAR_DIR = lib
CP = .$(SEP)$(JAR_DIR)/jackson-databind-2.15.2.jar$(SEP)$(JAR_DIR)/jackson-core-2.15.2.jar$(SEP)$(JAR_DIR)/jackson-annotations-2.15.2.jar

all:
	javac -cp "$(CP)" Scripts/*.java

run:
	java -cp "$(CP)" Scripts/Main

clean:
	$(RM) $(call FIXPATH,Scripts/*.class)