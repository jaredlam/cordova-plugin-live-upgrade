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
    
    [[GetUpdateInfoHelper shareInstance] getUpdateInfo];
    
}

@end
