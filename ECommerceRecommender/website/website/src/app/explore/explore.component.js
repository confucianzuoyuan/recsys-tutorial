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
var router_1 = require("@angular/router");
var http_1 = require("@angular/common/http");
var login_service_1 = require("../services/login.service");
var ExploreComponent = (function () {
    function ExploreComponent(loginService, router, httpService) {
        this.loginService = loginService;
        this.router = router;
        this.httpService = httpService;
        this.movies = [];
    }
    ExploreComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.router.params.subscribe(function (params) {
            if (params['type'] == "search") {
                _this.title = "搜索结果：";
                if (params['query'].trim() != "")
                    _this.getSearchMovies(params['query']);
            }
            else if (params['type'] == "guess") {
                _this.title = "猜你喜欢：";
                _this.getGuessMovies();
            }
            else if (params['type'] == "hot") {
                _this.title = "热门推荐：";
                _this.getHotMovies();
            }
            else if (params['type'] == "new") {
                _this.title = "新片发布：";
                _this.getNewMovies();
            }
            else if (params['type'] == "rate") {
                _this.title = "评分最多：";
                _this.getRateMoreMovies();
            }
            else if (params['type'] == "wish") {
                _this.title = "我喜欢的：";
                _this.getWishMovies();
            }
            else if (params['type'] == "genres") {
                _this.title = "影片类别：" + params['category'];
                _this.getGenresMovies(params['category']);
            }
            else if (params['type'] == "myrate") {
                _this.title = "我的评分电影：";
                _this.getMyRateMovies();
            }
        });
    };
    ExploreComponent.prototype.getMyRateMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/myrate?username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getGenresMovies = function (category) {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/search?query=' + category)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getSearchMovies = function (query) {
        var _this = this;
        console.log(query);
        this.httpService
            .get('http://localhost:8088/rest/movie/search?query=' + query)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getGuessMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/guess?num=-1&username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getHotMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/hot?num=-1&username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getNewMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/new?num=-1&username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getRateMoreMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/rate?num=-1&username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent.prototype.getWishMovies = function () {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/wish?num=-1&username=' + this.loginService.user.username)
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.movies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    ExploreComponent = __decorate([
        core_1.Component({
            selector: 'app-explore',
            templateUrl: './explore.component.html',
            styleUrls: ['./explore.component.css']
        }),
        __metadata("design:paramtypes", [login_service_1.LoginService, router_1.ActivatedRoute, http_1.HttpClient])
    ], ExploreComponent);
    return ExploreComponent;
}());
exports.ExploreComponent = ExploreComponent;
//# sourceMappingURL=explore.component.js.map