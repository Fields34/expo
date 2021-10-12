
open class BaseSharedObject {
  required public init() {}
}

public typealias SharedObject = AnySharedObject & BaseSharedObject & AnyMethodArgument

public class DemoModule: Module {
  public static func definition() -> ModuleDefinition {
    name("DemoModule")

    method("createObject") {
      return DemoObject()
    }

    method("destroyObject") { (module, object: DemoObject) in
      object.destroy()
    }

    objects([DemoObject.self])
  }
}

public class DemoObject: SharedObject {
  public static func definition() -> SharedObjectDefinition {
//    name("DemoObject")

//    constructor {
//      print("new DemoObject() has been called in JS")
//    }

//    method("increment") { demoObject in
//      demoObject.counter += 1
//    }
  }

  var counter = 0

  func destroy() {
    print("Goodbye!")
  }
}
