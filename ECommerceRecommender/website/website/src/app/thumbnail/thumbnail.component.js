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
var ThumbnailComponent = (function () {
    function ThumbnailComponent() {
        this.detail = false;
        this.movie = new movie_1.Movie;
    }
    ThumbnailComponent.prototype.ngOnInit = function () {
    };
    ThumbnailComponent.prototype.hover = function () {
        this.detail = true;
    };
    ThumbnailComponent.prototype.leave = function () {
        this.detail = false;
    };
    __decorate([
        core_1.Input(),
        __metadata("design:type", movie_1.Movie)
    ], ThumbnailComponent.prototype, "movie", void 0);
    ThumbnailComponent = __decorate([
        core_1.Component({
            selector: 'app-thumbnail',
            templateUrl: './thumbnail.component.html',
            styleUrls: ['./thumbnail.component.css']
        }),
        __metadata("design:paramtypes", [])
    ], ThumbnailComponent);
    return ThumbnailComponent;
}());
exports.ThumbnailComponent = ThumbnailComponent;
//# sourceMappingURL=thumbnail.component.js.map
