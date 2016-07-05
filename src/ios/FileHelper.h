//
//  FileHelper.h
//  云筑劳务
//
//  Created by yzw on 16/7/4.
//
//

#import <Foundation/Foundation.h>


@interface FileHelper : NSObject

+ (BOOL)haveTempVersion;
+ (void)updateTempVersion:(BOOL)bSuccess;

+ (NSString *)libPrefrePath;
+ (NSString *)libCachePath;

+ (NSString *)getUnZipPath;
+ (NSString *)getVersionPath;

+ (BOOL )startFromLocal;


// 将解压后的文件移到启动路径
+ (void)moveTemperVersionToRealPath;

+(BOOL)unzip:(NSString*)zipPath tounZipPath:(NSString*)unzipPath;

@end
