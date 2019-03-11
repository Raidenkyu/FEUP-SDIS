JFLAGS = -g
JC = javac
CLASSPATH = -classpath src/
BIN = -d bin/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(BIN) $(JFLAGS) $(CLASSPATH) $*.java
 
CLASSES = \
    src/client/Client.java \
    src/server/Server.java \
    src/server/ServerRegistry.java
 
default: classes
 
classes: $(CLASSES:.java=.class)
 
clean:
	$(RM) *.class

rmi: 
	rmiregistry -J-Djava.rmi.server.codebase=file:///home/joao/Documentos/SDIS/Lab3/bin/