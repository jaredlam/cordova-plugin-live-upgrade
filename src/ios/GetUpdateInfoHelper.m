//
//  GetUpdateInfoHelper.m
//  云筑劳务
//
//  Created by yzw on 16/7/4.
//
//


#import "GetUpdateInfoHelper.h"
#import "FileHelper.h"
#import "NetworkObject.h"
#import "MainViewController.h"
#import "AppDelegate.h"


//#define kUpdateUrl @"http://172.16.0.246:8092/upgrade_manifest.json"
#define kIngoreVersion @"kIngoreVersion"



@implementation UpdateModel

@end


@interface GetUpdateInfoHelper ()

@property (nonatomic, copy)CompleBlock compleBlock;
@property (nonatomic, strong)UpdateModel *updateModel;
@property (nonatomic, assign)BOOL bRequiredUpdate ;

@end

@implementation GetUpdateInfoHelper

+ (GetUpdateInfoHelper*)shareInstance {
    
    static GetUpdateInfoHelper *manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[GetUpdateInfoHelper alloc] init];
        
    });
    return manager;
}


- (NSArray*)getIgnoreVersions{
    
    NSArray *version = [[NSUserDefaults standardUserDefaults] objectForKey:kIngoreVersion];
    return version;
}

- (UpdateModel*)versionDicToModel:(NSDictionary*)dic {
    if (dic) {
        UpdateModel *model = [UpdateModel new];
        model.latest_version = [dic objectForKey:@"latest_version"];
        model.required_versions = [dic objectForKey:@"required_versions"];
        model.optional_versions = [dic objectForKey:@"optional_versions"];
        model.release_note = [dic objectForKey:@"release_note"];
        model.download_url = [dic objectForKey:@"download_url"];
        model.title = [dic objectForKey:@"title"];
        model.confirm_text = [dic objectForKey:@"confirm_text"];
        model.cancel_text = [dic objectForKey:@"cancel_text"];
        
        return model;
    }
    return nil;
}

- (void)showUpdateAlter :(UpdateModel*)model
                required:(BOOL)bRequiredUpdate{
    dispatch_async(dispatch_get_main_queue(), ^{
        UIAlertView *alterV = [[UIAlertView alloc] initWithTitle:model.title
                                                         message:model.release_note
                                                        delegate:self
                                               cancelButtonTitle:bRequiredUpdate?model.confirm_text:model.cancel_text
                                               otherButtonTitles:bRequiredUpdate?nil:model.confirm_text, nil];
        
        if (bRequiredUpdate) {
            alterV.tag = 1;
        }
        else{
            alterV.tag = 2;
        }
        [alterV show];
    });
    
}


- (void)showUnWifiAlter:(UpdateModel*)model
               required:(BOOL)bRequiredUpdate{
    dispatch_async(dispatch_get_main_queue(), ^{
        if(bRequiredUpdate){
            UIAlertView *alterV = [[UIAlertView alloc] initWithTitle:@"提示"
                                                             message:@"有版本要更新，当前是非Wi-Fi模式，下载将使用您的流量，建议您切换到Wi-Fi模式进行更新，点击确定将开始更新"
                                                            delegate:self
                                                   cancelButtonTitle:@"确定"
                                                   otherButtonTitles:nil];
            
            alterV.tag = 3;
            [alterV show];
        }
        else{
            UIAlertView *alterV = [[UIAlertView alloc] initWithTitle:@"提示"
                                                             message:@"有版本要更新，当前是非Wi-Fi模式，下载将使用您的流量，建议您切换到Wi-Fi模式进行更新，点击确定将开始更新"
                                                            delegate:self
                                                   cancelButtonTitle:@"取消"
                                                   otherButtonTitles:@"确定",nil];
            
            alterV.tag = 4;
            [alterV show];
        }
        
    });
    
}


- (void)getUpdateInfo:(NSString*)curentVersion
            updateUrl:(NSString*)url
  ignorCurrentVersion:(BOOL)ignorCurrentVersion
           comple:(CompleBlock)blcok{
    
    self.compleBlock = blcok;
    
    if ([FileHelper startFromLocal]) {// 从本地文件夹启动
        [self updateVersionSuccess];
    }
    
    if(ignorCurrentVersion){
        [[NSUserDefaults standardUserDefaults] removeObjectForKey:kIngoreVersion];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }
    
    
    self.updateModel = nil;
    self.bRequiredUpdate = NO;
    __weak __typeof(self)weakSelf = self;
    
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSData *data = [NSData dataWithContentsOfURL:[NSURL URLWithString:url]];
        
        if (data) {
            NSDictionary *dic = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:nil];
            
            __block UpdateModel *model = [self versionDicToModel:dic];
            self.updateModel = model;
            
            if ([self getIgnoreVersions] && [[self getIgnoreVersions] containsObject:model.latest_version]) {
                return ;
            }
            
            if (![curentVersion isEqualToString:model.latest_version]) {//和服务器上最新版本不一致并且服务器版本号不在曾经被忽略的版本中
                
                if([model.required_versions containsObject:curentVersion]||
                   [model.optional_versions containsObject:curentVersion]){ // 如果在必升和选升版本中有当前版本号就检查本地已下载的包是否是最新的
                    
                    self.bRequiredUpdate = [model.required_versions containsObject:curentVersion];
                    
                    if([NetworkObject isWifiConnect]){//wifi
                        
                        [self downHtmlZip:self.updateModel.download_url
                                   comple:^(BOOL bSuccess) {
                                       if (bSuccess) {
                                           [weakSelf showUpdateAlter:weakSelf.updateModel required:weakSelf.bRequiredUpdate];
                                       }
                                   
                                   }];
                    }
                    else if ([NetworkObject isWWANConnect]){//2G,3G,4G...
                        [self showUnWifiAlter:model
                                     required:_bRequiredUpdate];
                    }
                    
                    
                }
            }
            if(weakSelf.compleBlock){
                weakSelf.compleBlock(true);
            }
            
        }
        else{
            if(weakSelf.compleBlock){
                weakSelf.compleBlock(false);
            }
        }
    });
}


- (void)starDownzip{
    __weak __typeof(self)weakSelf = self;
    
    [self downHtmlZip:self.updateModel.download_url
               comple:^(BOOL bSuccess) {
                   if (bSuccess) {
                       [weakSelf updateLocalVersion];
                   }
               }];
}
-(void)downHtmlZip:(NSString*)downUrl comple:(void (^)(BOOL bSuccess))compleBlock{
    
    if (!downUrl) {
        return;
    }
    
    NSURL* url = [NSURL URLWithString:downUrl];
    // 得到session对象
    NSURLSession* session = [NSURLSession sharedSession];
    
    // 创建任务
    NSURLSessionDownloadTask* downloadTask = [session downloadTaskWithURL:url completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
        
        BOOL bsuccess=  [FileHelper unzip:location.path tounZipPath:[FileHelper getUnZipPath]];
        
        if (compleBlock) {
            compleBlock(bsuccess);
        }
    }];
    
    // 开始任务
    [downloadTask resume];
}

- (void)updateVersionSuccess{
    
    AppDelegate *appDelegate = [[UIApplication  sharedApplication] delegate];
    
    MainViewController *viewController = (MainViewController*)appDelegate.viewController;
    if (viewController) {
        viewController.wwwFolderName = [NSString stringWithFormat:@"file://%@",[FileHelper getVersionPath]];
        viewController.startPage =  @"index.html";
        NSURL *appURL = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%@", viewController.wwwFolderName, viewController.startPage]];
        NSURLRequest* appReq = [NSURLRequest requestWithURL:appURL cachePolicy:NSURLRequestUseProtocolCachePolicy timeoutInterval:20.0];
        [viewController.webViewEngine loadRequest:appReq];
        
    }
    else{
        MainViewController *viewController = [[MainViewController alloc] init];
        viewController.wwwFolderName = [NSString stringWithFormat:@"file://%@",[FileHelper getVersionPath]];
        viewController.startPage =  @"index.html";
        
        AppDelegate *appDelegate = [[UIApplication  sharedApplication] delegate];
        appDelegate.viewController = viewController;
        appDelegate.window.rootViewController = viewController;
        [appDelegate.window makeKeyAndVisible];
    }
    
}


- (void)updateLocalVersion{
    
    [FileHelper moveTemperVersionToRealPath];
    
    [self moveLastVersionToIngore];
    [self updateVersionSuccess];
    
    self.updateModel = nil;
    self.bRequiredUpdate = NO;
}

// 将最新的版本号加入被忽略版本号中
- (void)moveLastVersionToIngore{
    NSMutableArray *ignoreVersions =[NSMutableArray array];
    if (![self getIgnoreVersions]) {
        [ignoreVersions addObjectsFromArray:[self getIgnoreVersions]];
    }
    if (![ignoreVersions containsObject:_updateModel.latest_version]) {
        [ignoreVersions addObject:_updateModel.latest_version];
    }
    [[NSUserDefaults standardUserDefaults] setObject:ignoreVersions forKey:kIngoreVersion];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{
    
    if (alertView.tag == 1 && buttonIndex == 0) {//强制更新
        [self updateLocalVersion];
    }
    else if(alertView.tag == 2 && buttonIndex == 1){// 更新
        [self updateLocalVersion];
        
    }
    else if(alertView.tag == 2 && buttonIndex == 0){// 不更新
        
        [[NSFileManager defaultManager] removeItemAtPath:[FileHelper getUnZipPath]
                                                   error:nil];
        [self moveLastVersionToIngore];
        
    }
    else if(alertView.tag == 3 && buttonIndex == 0){// 下载
        [self starDownzip];
    }
    else if(alertView.tag == 4 && buttonIndex == 1){// 下载
        [self starDownzip];
    }
}

@end
