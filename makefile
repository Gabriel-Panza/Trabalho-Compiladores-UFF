all:
	javac Scripts/*.java

run:
	java Scripts/Main

clean:
	rm -f Scripts/*.class