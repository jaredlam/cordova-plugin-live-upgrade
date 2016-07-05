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
    
    [[GetUpdateInfoHelper shareInstance] getUpdateInfo:version updateUrl:url];
    
}

@end
