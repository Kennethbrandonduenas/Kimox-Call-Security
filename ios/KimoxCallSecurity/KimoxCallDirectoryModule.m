#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(KimoxCallDirectoryModule, NSObject)

RCT_EXTERN_METHOD(sync:(NSArray *)blocked
                  labels:(NSDictionary *)labels
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(status:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(entries:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
