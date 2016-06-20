var exec = require('cordova/exec');

module.exports = {
    sync: function (onSuccess, onError) {
        exec(onSuccess, onError, "AppUpdate", "Update", []);
    }
};
