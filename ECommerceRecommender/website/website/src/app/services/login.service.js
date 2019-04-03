"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
var core_1 = require("@angular/core");
var user_1 = require("../model/user");
var router_1 = require("@angular/router");
var http_1 = require("@angular/common/http");
var LoginService = (function () {
    function LoginService(router, http) {
        this.user = new user_1.User;
        this.data = new Object;
        this.user = new user_1.User;
        this.user.id = 1;
        this.user.username = '543293730@qq.com';
        this.user.password = '543293730@qq.com';
        /*var ls = this;
        this.router = router;
        this.http = http;
        this.data['success'] = true;
        this.router.events.subscribe({
          next: function (x) {
            if(x instanceof  NavigationStart){
              if( x.url.trim()=="/login" || x.url.trim()=="/register"){
    
              }else{
                if(!ls.isLogin()){
                  ls.router.navigate(['/login']);
                }
              }
            }
          } ,
          error: err => console.error('something wrong occurred: ' + err),
          complete: () => console.log('done'),
        });*/
    }
    LoginService.prototype.isLogin = function () {
        if (this.user.id == null)
            return false;
        return true;
    };
    LoginService.prototype.login = function (user) {
        var _this = this;
        /*this.user = new User;
        this.user.id = 1;
        this.user.username = "543293730@qq.com";
        this.user.password = "";
        this.router.navigate(['/home']);*/
        this.http
            .get('http://localhost:8088/rest/users/login?username=' + user.username + '&password=' + user.password)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.user = new user_1.User;
                _this.user.id = data['id'];
                _this.user.username = user.username;
                _this.user.password = user.password;
                _this.router.navigate(['/home']);
            }
            _this.data = data;
        }, function (err) {
            console.log('Somethi,g went wrong!');
            _this.data['success'] = false;
            _this.data['message'] = '服务器错误！';
        });
    };
    LoginService.prototype.register = function (user) {
        var _this = this;
        //this.router.navigate(['/login']);
        this.http
            .get('http://localhost:8088/rest/users/register?username=' + user.username + '&password=' + user.password)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.router.navigate(['/login']);
            }
            _this.data = data;
        }, function (err) {
            console.log('Something went wrong!');
            _this.data['success'] = false;
            _this.data['message'] = '服务器错误！';
        });
    };
    LoginService.prototype.logout = function () {
        this.user = new user_1.User;
        this.router.navigate(['/login']);
    };
    LoginService = __decorate([
        core_1.Injectable(),
        __metadata("design:paramtypes", [router_1.Router, http_1.HttpClient])
    ], LoginService);
    return LoginService;
}());
exports.LoginService = LoginService;
//# sourceMappingURL=login.service.js.map