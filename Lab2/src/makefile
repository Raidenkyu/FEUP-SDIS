JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
    client/Client.java \
    server/Server.java \
    server/RegisteryServer.java \
    server/MulticastServer.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) $(CLASSES:.java=.class)