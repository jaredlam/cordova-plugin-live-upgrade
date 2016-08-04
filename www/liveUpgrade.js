var exec = require('cordova/exec');

module.exports = {
    
    sync: function (localVersion, manifestUrl, forceCheck, onSuccess, onError) {
    exec(onSuccess, onError, "AppUpdate", "Update", [localVersion, manifestUrl, forceCheck]);
    }
};
