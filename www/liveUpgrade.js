var exec = require('cordova/exec');

module.exports = {
 	Update: function(message, completeCallback, title, buttonLabel) {
               exec(onSuccess, onError, "AppUpdate", "Update", [localVersion, manifestUrl]);
    }
};
