import java.util.regex.Pattern

// no semicolons
// prefer
def str = 'hello'
// do not
String str2 = 'hello';

// simple definition
def list = [1, 2, 3, 4, 5]
def map = [a: 1, b: 2, c: 3]
// do not
def list1 = List.of(1, 2, 3)
def map1 = Map.of('a', 1, 'b', 2, 'c', 3)

// use 'def' to define variable, not var or class
def a = 1
// do not
var b = 2
int c = 3

// public by default
class Test1 {
    String name

    String hello() {
        'hello ' + name
    }
}
// do not
public class Test2 {
    private String name

    public String hello() {
        'hello ' + name
    }
}

// omitting parentheses
list.each { println it }
// do not
list.each(it -> {
    println it
})

// use operator overload
// compare to
def r = 1 <=> 2
// left shift
list << 6
def sb = new StringBuilder()
sb << 'hello'
println sb

// use groovy methods first
def longValue = 'abc' * 100
// do not
def sb2 = new StringBuilder()
100.times {
    sb2 << 'abc'
}
def longValue2 = sb2.toString()

// use groovy methods first
def paddingValue = 'abc'.padLeft(16, ' ')
// do not use third party library
def paddingValue2 = com.google.common.base.Strings.padStart('abc', 16, (char) ' ')

// need not return when last line
String getName() {
    'hello'
}
// do not
String getName2() {
    return 'hello'
}

// do not define method / function parameter without given type
// prefer
def getName3(String name) {
    name
}
// do not
def getName4(name) {
    name
}

// use groovy stream methods first
list.collect { it * 2 }.each { println it }
// do not use java stream

// use groovy pattern
def pat = ~/[a-z]+/
// do not use java pattern
def pat2 = Pattern.compile('[a-z]+')

// use groovy truth check
def empty = ''
if (empty) {
    println 'not empty'
}
// do not
if (empty != null && !empty.isEmpty()) {
    println 'not empty'
}
// prefer
def list2 = [1, 2, 3]
if (list2) {
    println 'not empty'
}
// do not
if (!list2.isEmpty()) {
    println 'not empty'
}
// prefer
def map2 = [a: 1, b: 2, c: 3]
if (map2) {
    println 'not empty'
}
// do not
if (!map2.isEmpty()) {
    println 'not empty'
}

// use simple getter and setter
class Person {
    String name
}

def person = new Person()
// prefer
person.name = 'hello'
// do not
person.setName('hello')
// prefer
assert person.name == 'hello'
// do not
assert person.getName() == 'hello'

// use ?:
def name = person.name ?: 'world'
// do not
def name2 = person.name == null ? 'world' : person.name

// use == instead of equals
assert name == name2
// do not
assert name.equals(name2)

// use GString when long string
def longStr = """
${name}
xxx
yyy
"""
// do not use + to concat string
def longStr2 = name + '\n' + 'xxx' + '\n' + 'yyy'

// use ?. when null
Person person3 = null
println person3?.name
// do not
if (person3 != null) {
    println person3.name
}