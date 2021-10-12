// Copyright 2018-present 650 Industries. All rights reserved.

#import <ExpoModulesCore/JSIConversions.h>
#import <ExpoModulesCore/JSIObject.h>

@implementation JSIObject {
  std::weak_ptr<jsi::Runtime> _runtime;
  std::shared_ptr<jsi::Object> _object;
  std::shared_ptr<CallInvoker> _callInvoker;
}

- (instancetype)initFrom:(std::shared_ptr<jsi::Object>)object
             withRuntime:(std::shared_ptr<jsi::Runtime>)runtime
             callInvoker:(std::shared_ptr<react::CallInvoker>)callInvoker
{
  if (self = [super init]) {
    _runtime = runtime;
    _object = object;
    _callInvoker = callInvoker;
  }
  return self;
}

- (jsi::Object *)get
{
  return _object.get();
}

- (void)setProperty:(nonnull NSString *)key toValue:(nullable JSIObject *)value
{
  auto runtime = _runtime.lock();

  if (runtime) {
    _object->setProperty(*runtime, [key UTF8String], *[value get]);
  }
}

- (nullable id)objectForKeyedSubscript:(NSString *)key
{
  auto runtime = _runtime.lock();

  if (runtime) {
    auto value = _object->getProperty(*runtime, [key UTF8String]);
    return expo::convertJSIValueToObjCObject(*runtime, value, _callInvoker);
  }
  return nil;
}

- (void)setObject:(nullable id)obj forKeyedSubscript:(NSString *)key
{
  auto runtime = _runtime.lock();

  if (runtime) {
    _object->setProperty(*runtime, [key UTF8String], expo::convertObjCObjectToJSIValue(*runtime, obj));
  }
}

- (void)setFunction:(nonnull NSString *)key block:(JSIFunctionBlock)block
{
  auto runtime = _runtime.lock();

  if (!runtime) {
    return;
  }

  auto propId = jsi::PropNameID::forAscii(*runtime, [key UTF8String]);
  auto callInvoker = _callInvoker;
  auto jsiFn = jsi::Function::createFromHostFunction(*runtime, propId, 2, [callInvoker, block](jsi::Runtime &rt, const jsi::Value &thisVal, const jsi::Value *args, size_t count) -> jsi::Value {
    NSMutableArray *argsArray = [NSMutableArray arrayWithCapacity:count];
    for (int i = 0; i < count; i++) {
      argsArray[i] = expo::convertJSIValueToObjCObject(rt, args[i], callInvoker);
    }
    return expo::convertObjCObjectToJSIValue(rt, block(argsArray));
  });

  _object->setProperty(*runtime, [key UTF8String], jsiFn);
}

@end
