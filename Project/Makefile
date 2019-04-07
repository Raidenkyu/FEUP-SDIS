JFLAGS = -g
JC = javac
CLASSPATH = -classpath src/
BIN = -d bin/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(BIN) $(JFLAGS) $(CLASSPATH) $*.java
 
CLIENTCLASSES = \
    src/client/Client.java \

SERVERCLASSES = \
	src/peer/Chunk.java \
    src/peer/Peer.java \
	src/peer/PeerRMI.java \
	src/peer/Worker.java \
	src/peer/PeerChannel.java \
	src/peer/PeerStorage.java
 
default: classes
 
classes: $(CLIENTCLASSES:.java=.class)\
$(SERVERCLASSES:.java=.class)

client : $(CLIENTCLASSES:.java=.class)

server : $(SERVERCLASSES:.java=.class)
 
clean:
	$(RM) *.class

rmi: 
	rmiregistry -J-Djava.rmi.server.codebase=file:///home/joao/Documentos/SDIS/Project/bin/