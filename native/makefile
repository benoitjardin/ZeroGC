OS ?= $(shell uname -s)
MACH ?= $(shell uname -m)

JDK_HOME_Linux := /usr/lib/jvm/java-6-openjdk-i386
JDK_HOME_Windows_NT := /cygdrive/c/dev/jdk-6u35/
JDK_HOME_CYGWIN_NT-5.1 := /cygdrive/c/dev/jdk1.6.0_35/
JDK_HOME ?= ${JDK_HOME_${OS}}

JAVAC := "${JDK_HOME}/bin/javac"
JAR := "${JDK_HOME}/bin/jar"

CPPFLAGS_Linux := -I${JDK_HOME}/include/linux
CPPFLAGS_SunOS := -I${JDK_HOME}/include/solaris
CPPFLAGS_Windows_NT := -I"${JDK_HOME}"/include/win32
CPPFLAGS := -I"${JDK_HOME}"/include ${CPPFLAGS_${OS}}

#CFLAGS_Linux_x86_64 := -m32 -Wa,-32
CFLAGS_Linux := ${CFLAGS_${OS}_${MACH}} -fPIC
CFLAGS_Windows_NT := -mno-cygwin ${CFLAGS_${OS}_${MACH}} -std=c99
CFLAGS := ${CFLAGS_${OS}}

#LDFLAGS_Linux_x86_64 := -m32
LDFLAGS_Linux := ${LDFLAGS_${OS}_${MACH}}
LDFLAGS_Windows_NT := -mno-cygwin -Wl,--add-stdcall-alias
LDFLAGS := ${LDFLAGS_${OS}}

LDLIBS_Windows_NT := -lws2_32
LDLIBS += ${LDLIBS_${OS}}

LIB_Linux := lib
LIB_Windows_NT :=
LIB_CYGWIN_NT-5.1 :=
LIB := ${LIB_${OS}}
SO_Linux := so
SO_Windows_NT := dll
SO_CYGWIN_NT-5.1 :=
SO := ${SO_${OS}}

all: ${LIB}zerogc.${SO}

echo:
	echo ${CPPFLAGS}

${LIB}zerogc.${SO}: zerogc.o
	$(CC) $(LDFLAGS) -shared -o $@ $(LOADLIBES) $^ $(LDLIBS)

clean: 
	rm ${LIB}zerogc.${SO} zerogc.o
