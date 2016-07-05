//
//  NetworkObject.m
//  云筑劳务
//
//  Created by yzw on 16/7/5.
//
//

#import "NetworkObject.h"
#import "Reachability.h"

#define NetWorkHostname @"www.baidu.com"

@implementation NetworkObject

+(BOOL)isWifiConnect{
    

    Reachability *hostReach = [Reachability reachabilityWithHostname:NetWorkHostname];
    NetworkStatus status = [hostReach currentReachabilityStatus];
    
    if(status == ReachableViaWiFi)
    {
        return YES;
    }
    return NO;
}

+(BOOL)isWWANConnect{
    
    
    Reachability *hostReach = [Reachability reachabilityWithHostname:NetWorkHostname];
    NetworkStatus status = [hostReach currentReachabilityStatus];
    
    if(status == ReachableViaWWAN)
    {
        return YES;
    }
    return NO;
}


@end

