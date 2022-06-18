# Regular Bytecode Expression (regbex)

> ⚠️ This library should only be used in Kotlin - at least for Regbex builders.

> ⚠️ This repository is in experimental state! If you encounter any issue, please open a GitHub issue or pull request!

A regular expression engine that's made for Java bytecode and objectweb ASM.


## Missing Features / Known Issues
1. Look-Behind & Look-After (Won't add for now) (Can be replaced with `thenCheckWithoutMovingPointer`, but 
negative look-around would be impossible. Will add that later)


## Using
### Getting Started
Use jitpack:

Gradle (Kotlin)
```kotlin
repositories {
    ...
    maven { url ("https://jitpack.io") }
}

dependencies {
    implementation("com.github.fan87:Regular-Bytecode-Expression:<version>")
}
```

Gradle (Groovy)
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.fan87:Regular-Bytecode-Expression:<version>'
}
```

Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.fan87</groupId>
    <artifactId>Regular-Bytecode-Expression</artifactId>
    <version><version></version>
</dependency>
```

You could always find all available versions in GitHub release tab

### Use Case
Let's say you want to do string obfuscation. Normally you would have to have a main for loop, a few if checks.
But not anymore! With this library, you can match every LdcString, and replace them to obfuscated string.

Not only that, let's say you want to match a `System.out.println("" /*Ldc String*/")`, normally you have to
save the current instruction index, have a main for loop, a lot of if check, but now you could just match 
getstatic, ldcstring, and method call, capture the ldc string, and that's where and what the string is, no for loop,
only a few lines of code

### Usage
You can check the [Example Test Class](src/test/kotlin/Examples.kt) for code example and usage.

### Debugging
Struggling to debug what's wrong with your regbex? You can enable debug mode by setting `matcher.debug` to true
(Or in java, `matcher.setDebug(true)`). It will print out all regbex elements, current regbex index, all instructions,
current instructions index, and most importantly, the fail reason.

You could also just print out `matcher.debugMessage` (Or in java, `matcher.getDebugMessage()`) if you don't want
to print all debug messages out to terminal

### Using it in Java
If your codebase is in Java and you don't want to re-code your entire project, you can have classes in Kotlin that pre-defines
every regbex you'll be using.

Attempting to build regbex pattern in Java is still technically possible, but it will make your code un-readable, and 
you shouldn't do that.

### Efficiency
Just like regular expression, it's not designed to be high efficient, but to make your life easier.

In theory, it should be as fast as regex engines, bcs what I'm doing is (probably) how most regex engines are
implemented.

### Confused?
Please read the KDoc (Like JavaDoc, but for Kotlin) if any function is confusing you. For example, using `TypeExpression`
class to match a type using regular expression could be confusing because you must assume the input could have `L` in
the front and `;` in the end of the class name, so if you want to match `java.lang`, you have to do `L?java/lang/.*;?`


## FAQ
### How to ignore let's say all Label Node?
Not Supported Yet

### What is lazy
Lazy in regex is `?`, for example, `.*?`, it means that it will match as few as possible.

### Is it thread-safe?
About the matcher, no, not at all. Please create multiple matcher instances to match instructions in a different
thread, even if they are the same.

## Development / Code Quality
### About Testing
I'm not aiming to have coverage 100%, it's absolute time-wasting to me to write that many tests when I have confident
that the code will work. So you may find the test quality to be a little worse than others.