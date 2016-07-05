//
//  GetUpdateInfoHelper.h
//  云筑劳务
//
//  Created by yzw on 16/7/4.
//
//

#import <Foundation/Foundation.h>

#define  kUpdateNotification @"kUpdateNotification"

@interface UpdateModel : NSObject

@property (nonatomic, strong) NSString *latest_version; //最新版本号
@property (nonatomic, strong) NSArray *required_versions; //强制更新的版本号
@property (nonatomic, strong) NSArray *optional_versions; //选择更新的版本号
@property (nonatomic, strong) NSString *release_note; //更新说明
@property (nonatomic, strong) NSString *download_url; //更新地址
@property (nonatomic, strong) NSString *title; //更新title
@property (nonatomic, strong) NSString *confirm_text; //确认按钮文字
@property (nonatomic, strong) NSString *cancel_text;  //取消按钮文字

@end

@interface GetUpdateInfoHelper : NSObject

+ (GetUpdateInfoHelper*)shareInstance;

- (void)getUpdateInfo;


@end
