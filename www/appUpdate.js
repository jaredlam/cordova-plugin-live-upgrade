var exec = require('cordova/exec');

module.exports = {
    Download: function (onSuccess, onError) {
        exec(onSuccess, onError, "AppUpdate", "Update", []);
    }
};
