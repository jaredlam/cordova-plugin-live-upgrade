//
//  AppUpdate.m
//  云筑劳务
//
//  Created by yzw on 16/7/5.
//
//

#import "AppUpdate.h"
#import "GetUpdateInfoHelper.h"

@implementation AppUpdate

- (void)Update:(CDVInvokedUrlCommand*)command
{
    NSString* version = [command.arguments objectAtIndex:0];
    NSString* url = [command.arguments objectAtIndex:1];
    
    __block NSString* callbackId = command.callbackId;
    
    BOOL ignorCurrentVersion = false;
 
    if(command.arguments.count>2 && ![[command.arguments objectAtIndex:2] isKindOfClass:[NSNull class]]){
        ignorCurrentVersion = [[command.arguments objectAtIndex:2] boolValue];
    }

    __weak __typeof(self)weakSelf = self;
    [[GetUpdateInfoHelper shareInstance] getUpdateInfo:version
                                             updateUrl:url
                                   ignorCurrentVersion:ignorCurrentVersion
                                                comple:^(BOOL bSuccess) {
                                                    if (bSuccess) {
                                                        CDVPluginResult *reuslt = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                                                        
                                                        [weakSelf.commandDelegate sendPluginResult:reuslt callbackId:callbackId];
                                                    }
                                                    else{
                                                        CDVPluginResult *reuslt = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
                    
                                                         [weakSelf.commandDelegate sendPluginResult:reuslt callbackId:callbackId];
                                                    }
                                                }];
    
}

@end
