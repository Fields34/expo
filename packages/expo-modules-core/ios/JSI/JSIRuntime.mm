// Copyright 2018-present 650 Industries. All rights reserved.

#import <jsi/jsi.h>
#import <ReactCommon/CallInvoker.h>
#import <ExpoModulesCore/JSIRuntime.h>
#import <ExpoModulesCore/ExpoModulesHostObject.h>

#if __has_include(<ExpoModulesCore/ExpoModulesCore-Swift.h>)
// When `use_frameworks!` is used in the Podfile, the generated Swift header is not local.
#import <ExpoModulesCore/ExpoModulesCore-Swift.h>
#else
#import "ExpoModulesCore-Swift.h"
#endif

using namespace facebook;

@interface RCTBridge (JSI)
- (void *)runtime;
- (std::shared_ptr<facebook::react::CallInvoker>)jsCallInvoker;
@end

@implementation JSIRuntime {
  std::shared_ptr<jsi::Runtime> _runtime;
  std::shared_ptr<react::CallInvoker> _jsCallInvoker;
  std::shared_ptr<jsi::Object> _expoModulesObject;

  SwiftInteropBridge *_mediator;
}

- (instancetype)initWithBridge:(nonnull RCTBridge *)bridge mediator:(nonnull SwiftInteropBridge *)mediator
{
  if (self = [super init]) {
    _jsCallInvoker = bridge.jsCallInvoker;
    _runtime = std::shared_ptr<jsi::Runtime>([bridge respondsToSelector:@selector(runtime)] ? reinterpret_cast<jsi::Runtime *>(bridge.runtime) : nullptr);
    _mediator = mediator;
    [self prepareRuntime];
  }
  return self;
}

- (jsi::Runtime *)runtime
{
  return _runtime.get();
}

- (void)prepareRuntime
{
  if (_expoModulesObject) {
    // Object was initialized, so the runtime is already "prepared".
    return;
  }

  auto runtime = [self runtime];
  auto hostObject = std::make_shared<expo::ExpoModulesHostObject>(_mediator);

  auto main = jsi::Object::createFromHostObject(*runtime, hostObject);

  runtime->global()
    .setProperty(*runtime, "ExpoModules", main);
}

- (nonnull JSIObject *)global
{
  auto global = std::make_shared<jsi::Object>(_runtime->global());
  return [[JSIObject alloc] initFrom:global withRuntime:_runtime callInvoker:_jsCallInvoker];
}

- (nonnull JSIObject *)mainObject
{
  auto global = std::make_shared<jsi::Object>(_runtime->global());
  auto main = std::make_shared<jsi::Object>(global->getProperty(*_runtime, "ExpoModules").asObject(*_runtime));
  return [[JSIObject alloc] initFrom:main withRuntime:_runtime callInvoker:_jsCallInvoker];
}

- (nonnull JSIObject *)createObject
{
  auto object = std::make_shared<jsi::Object>(jsi::Object(*_runtime.get()));
  return [[JSIObject alloc] initFrom:object withRuntime:_runtime callInvoker:_jsCallInvoker];
}

@end
