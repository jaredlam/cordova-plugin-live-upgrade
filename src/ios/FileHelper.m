//
//  FileHelper.m
//  云筑劳务
//
//  Created by yzw on 16/7/4.
//
//

#import "FileHelper.h"
#import "ZipArchive.h"


#define kUnZipDirectory @"zip"
#define kUpdateDirectory @"www"
#define kHaveTempUpdate @"kHaveTempUpdate"

@implementation FileHelper


+ (NSString *)libPrefrePath {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES);
    return [[paths objectAtIndex:0] stringByAppendingPathComponent:@"Preferences"];
}

+ (NSString *)libCachePath
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    return [paths objectAtIndex:0];
}

+ (NSString *)getUnZipPath {
    
    NSFileManager*fileManager =[NSFileManager defaultManager];
    NSString *filePath = [[FileHelper libCachePath] stringByAppendingPathComponent:kUnZipDirectory];
    if (![fileManager fileExistsAtPath:filePath]) {
        [fileManager createDirectoryAtPath:filePath
               withIntermediateDirectories:YES
                                attributes:nil
                                     error:nil];
    }
    return filePath;
}
+ (NSString *)getVersionPath {
    
    NSFileManager*fileManager =[NSFileManager defaultManager];
    NSString *filePath = [[FileHelper libPrefrePath] stringByAppendingPathComponent:kUpdateDirectory];
    if (![fileManager fileExistsAtPath:filePath]) {
        [fileManager createDirectoryAtPath:filePath
               withIntermediateDirectories:YES
                                attributes:nil
                                     error:nil];
    }
    return filePath;
}
+ (BOOL )startFromLocal{
    NSFileManager*fileManager =[NSFileManager defaultManager];
    NSString *filePath = [[FileHelper libPrefrePath] stringByAppendingPathComponent:kUpdateDirectory];
    return [fileManager fileExistsAtPath:filePath];
}


+ (BOOL)haveTempVersion {
    
    return [[NSUserDefaults standardUserDefaults] boolForKey:kHaveTempUpdate];
}

+ (void)updateTempVersion:(BOOL)bSuccess {
    [[NSUserDefaults standardUserDefaults] setBool:bSuccess forKey:kHaveTempUpdate];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

+(BOOL)unzip:(NSString*)zipPath tounZipPath:(NSString*)unzipPath{
    
    BOOL status = NO;
    
    NSFileManager*fileManager =[NSFileManager defaultManager];
    if (![fileManager fileExistsAtPath:zipPath]) {
        NSLog(@"zip file not found");
    }
    ZipArchive* zip = [[ZipArchive alloc] init];
    
    status = [zip UnzipOpenFile:zipPath];
    if(status)
    {
        status = [zip UnzipFileTo:unzipPath overWrite:YES];
        if(! status )
        {
            NSLog(@"========unzip html5 failed!=======");
        }
        
        
        [zip UnzipCloseFile];
    }
    
    
    zip = nil;
    
    [fileManager removeItemAtPath:zipPath error:nil];
    
    return status;
}

+ (void)moveTemperVersionToRealPath {
    NSFileManager *manager = [NSFileManager defaultManager];
  
    NSArray *dirArray = [manager contentsOfDirectoryAtPath:[self getUnZipPath] error:nil];
    
    if (dirArray && dirArray.count>0) {
        NSString *path = [[self getUnZipPath] stringByAppendingPathComponent:[dirArray firstObject]];
        
        
        NSArray *sourceArray = [manager contentsOfDirectoryAtPath:path error:nil];
        for (NSString *file in sourceArray) {
            
            NSString *sourcePath = [path stringByAppendingPathComponent:file];
            NSString *desFilePath = [[self getVersionPath] stringByAppendingPathComponent:file];
            
            NSError *erro = nil;
            BOOL bsuccess = [manager moveItemAtPath:sourcePath
                                             toPath:desFilePath
                                              error:&erro];
            
              NSLog(@"bSucc = %d,%@",bsuccess,erro);
        }
        
        [manager removeItemAtPath:[self getUnZipPath]
                            error:nil];
        [self updateTempVersion:NO];
    }

}

@end
