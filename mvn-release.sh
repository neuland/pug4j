#!/bin/bash
export GPG_TTY=$(tty)
mvn release:clean release:prepare release:perform -Darguments="-Dgpg.keyname=DCD30968 -Dgpg.passphrase=$JADE4J_PASS"
