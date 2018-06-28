
# Working on Java on MacOS


We use java 8, gradle for builds, and intellij as an IDE. Feel free to use something else as your IDE if you are so 
inclined, but IntelliJ rocks.

## Install java 8 on your mac

```
brew tap caskroom/versions
brew cask install java8
```

According to the "brew way" that should work.

But you might want to fall back to:
`
brew install caskroom/versions/java8
`

## Install gradle

`
brew install gradle
`

You'll also want to setup a JAVA_HOME so gradle will play nicely and compile using java 8.
```
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
```

## Install IntelliJ community edition

`
brew cask install intellij-idea-ce
`
After doing this you are pretty close to ready.

We'll just need to setup intellij, then checkout orion and get it working.

There is a stackoverflow that answers questions around this, but I'll link you to the order to do things in
First startup intellij, and then setup the default JDK:
https://stackoverflow.com/a/31420120

Then outside of intellij checkout orion as per the project readme using git directly.  Then open this folder in 
intellij.

You'll be presented with an import project screen, and series of steps to complete as per the linked to doc.  Set the 
path to Gradle will be something like: `/usr/local/Cellar/gradle/4.3.1/libexec/`
(based on your brew config and setup)


Once you've done this you should have an environment that is nicely setup for working with Orion.
