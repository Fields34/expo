
public protocol AnySharedObject: AnyObject {
  #if swift(>=5.4)
  @SharedObjectDefinitionBuilder
  static func definition() -> SharedObjectDefinition
  #else
  static func definition() -> SharedObjectDefinition
  #endif
}
