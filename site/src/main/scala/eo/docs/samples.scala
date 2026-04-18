package eo.docs

// Top-level ADT samples referenced by mdoc fences in site/docs/*.md.
//
// Macro-derived optics (`lens[…]` / `prism[…]`) need their target
// case classes / enum cases to live at a package-level location —
// mdoc wraps every fence in an anonymous object, and nested case
// classes trip the back-end on "missing outer accessor" when the
// macro tries to emit `new T(...)`. Hosting the samples here means
// site pages can freely show macro usage without hitting that.

final case class Zip(code: Int, extension: String)

final case class Address(street: String, zip: Zip)

final case class Person(name: String, address: Address)

final case class Config(name: String, timeout: Option[Int])

final case class App(config: Config)

final case class Phone(isMobile: Boolean, number: String)

final case class Owner(phones: List[Phone])

enum Event:
  case Click(x: Int, y: Int)
  case Scroll(delta: Int)

enum Shape:
  case Circle(r: Double)
  case Square(s: Double)
  case Triangle(b: Double, h: Double)

final case class Coords(x: Int, y: Int)

enum Shape2:
  case Circle(c: Coords, r: Double)
  case Square(c: Coords, s: Double)

final case class Customer(name: String, age: Int)

final case class NameAgePair(name: String, age: Int)
