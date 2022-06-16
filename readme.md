# WIP
This thing is still in progress, and planned to be finished on 6/17/22
# Regular Bytecode Expression (regbex)

> ⚠️ This library should only be used in Kotlin - at least for Regbex builders.

A regular expression engine that's made for Java bytecode and objectweb ASM.


## Missing Features / Known Issues
1. Look-Behind & Look-After (Won't add for now)
2. Nested capture group's order is incorrect (You can fix it by using name instead of the index of it, and you should do it)
3. Incorrect `next()` order (For example, input: `aaabbbccc`, and regex: `(aaabbb|bb)`, it would find `bb` first before
`aaabbb` because `bb` ends before `aaabbb`. I'm bad at writing algorithms so lol)

########## Since it's WIP, here's a list of all missing features in this version ##########

4. not
5. any amount of (`*`)
6. captured group (not capturing group, but using already captured group, equivalent to `\1`, `\2` in regex)


## Using
### Usage
You can check the [Example Test Class](src/test/kotlin/Examples.kt) for code example and usage.

### Using it in Java
If your codebase is in Java and you don't want to re-code your entire project, you can have classes in Kotlin that pre-defines
every regbex you'll be using.

Attempting to build regbex pattern in Java is still technically possible, but it will make your code un-readable, and 
you shouldn't do that.

### Efficiency
Just like regular expression, it's not designed to be high efficient, but to make your life easier.

### Confused?
Please read the KDoc (Like JavaDoc, but for Kotlin) if any function is confusing you. For example, using `TypeExpression`
class to match a type using regular expression could be confusing because you must assume the input could have `L` in
the front and `;` in the end of the class name, so if you want to match `java.lang`, you have to do `L?java/lang/.*;?`

## FAQ
### How to ignore let's say all Label Node?
Not Supported Yet

## Development / Code Quality
### About Testing
I'm not aiming to have coverage 100%, it's absolute time-wasting to me to write that many tests when I have confident
that the code will work. So you may find the test quality to be a little worse than others.