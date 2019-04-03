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
var movie_1 = require("../model/product");
var http_1 = require("@angular/common/http");
var login_service_1 = require("../services/login.service");
var tag_1 = require("../model/tag");
var router_1 = require("@angular/router");
require("rxjs/add/operator/switchMap");
var MdetailComponent = (function () {
    function MdetailComponent(route, httpService, loginService) {
        this.route = route;
        this.httpService = httpService;
        this.loginService = loginService;
        this.movie = new movie_1.Movie;
        this.sameMovies = [];
        this.myTags = [];
        this.movieTags = [];
    }
    MdetailComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.route.params
            .subscribe(function (params) {
            var id = params['id'];
            _this.getMovieInfo(id);
            _this.getSameMovies(id);
            _this.getMyTags(id);
            _this.getMovieTags(id);
        });
    };
    MdetailComponent.prototype.addMyTag = function (name) {
        var tag = new tag_1.Tag;
        tag.sum = 1;
        tag.name = name;
        this.myTags.push(tag);
        /*this.httpService
          .get('http://localhost:8088/rest/movie/newtag?username='+this.loginService.user.username+'&tagname='+name)
          .subscribe(
            data => {
              if(data['success'] == true){
                var tag = new Tag;
                tag.id=1;
                tag.name="abc1111";
                tag.sum=8;
    
                this.myTags.push(tag);
              }
            },
            err => {
              console.log('Somethi,g went wrong!');
            }
          );*/
    };
    MdetailComponent.prototype.removeMyTag = function (id) {
        for (var _i = 0, _a = this.myTags; _i < _a.length; _i++) {
            var myTag = _a[_i];
            if (myTag.id == id) {
                this.myTags.unshift(myTag);
                break;
            }
        }
    };
    MdetailComponent.prototype.getMyTags = function (id) {
        var tag = new tag_1.Tag;
        tag.id = 1;
        tag.name = "abc1111";
        tag.sum = 8;
        this.myTags.push(tag);
        /*this.httpService
          .get('http://localhost:8088/rest/movie/mytag/'+id+'?username='+this.loginService.user.username)
          .subscribe(
            data => {
              if(data['success'] == true){
                var tag = new Tag;
                tag.id=1;
                tag.name="abc1111";
                tag.sum=8;
    
                this.myTags.push(tag);
              }
            },
            err => {
              console.log('Somethi,g went wrong!');
            }
          );*/
    };
    MdetailComponent.prototype.getMovieTags = function (id) {
        var tag = new tag_1.Tag;
        tag.id = 1;
        tag.name = "abc";
        tag.sum = 8;
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        this.movieTags.push(tag);
        /*this.httpService
          .get('http://localhost:8088/rest/movie/tag/'+id)
          .subscribe(
            data => {
              if(data['success'] == true){
                var tag = new Tag;
                tag.id=1;
                tag.name="abc";
                tag.sum=8;
    
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
                this.movieTags.push(tag);
              }
            },
            err => {
              console.log('Somethi,g went wrong!');
            }
          );*/
    };
    MdetailComponent.prototype.getSameMovies = function (id) {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/same/' + id + '?num=6')
            .subscribe(function (data) {
            if (data['success'] == true) {
                _this.sameMovies = data['movies'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    MdetailComponent.prototype.getMovieInfo = function (id) {
        var _this = this;
        this.httpService
            .get('http://localhost:8088/rest/movie/info/' + id)
            .subscribe(function (data) {
            if (data['success'] == true) {
                /*var movie = new Movie;
                movie.id=1;
                movie.descri="Set in the 22nd century, The Matrix tells the story of a computer hacker who joins a group of underground insurgents fighting the vast and powerful computers who now rule the earth.";
                movie.issue="November 20, 2001";
                movie.language="English";
                movie.name="The Matrix";
                movie.picture="./assets/11.jpg";
                movie.score=6;
                movie.shoot="1999";
                movie.timelong="136 minutes";*/
                _this.movie = data['movie'];
            }
        }, function (err) {
            console.log('Somethi,g went wrong!');
        });
    };
    MdetailComponent = __decorate([
        core_1.Component({
            selector: 'app-mdetail',
            templateUrl: './mdetail.component.html',
            styleUrls: ['./mdetail.component.css']
        }),
        __metadata("design:paramtypes", [router_1.ActivatedRoute,
            http_1.HttpClient,
            login_service_1.LoginService])
    ], MdetailComponent);
    return MdetailComponent;
}());
exports.MdetailComponent = MdetailComponent;
//# sourceMappingURL=mdetail.component.js.map
