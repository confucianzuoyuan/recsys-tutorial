import { Injectable } from '@angular/core';
import {User} from "../model/user";
import {NavigationStart, Router} from "@angular/router";
import {HttpClient} from "@angular/common/http";
import {constant} from "../model/constant";

@Injectable()
export class LoginService {

  user: User = new User;
  router:Router;
  http:HttpClient;
  data = new Object;

  constructor(router:Router,http:HttpClient) {

    var ls = this;
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
    });
  }

  isLogin():boolean{
    if(this.user.userId == null)
      return false;
    return true;
  }

  login(user:User):void {
    this.http
      .get(constant.BUSSINESS_SERVER_URL+'rest/users/login?username='+user.username+'&password='+user.password)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.user = data['user'];
            this.router.navigate(['/home']);
          }
          this.data = data;

        },
        err => {
          console.log('Something went wrong!');
          this.data['success'] = false;
          this.data['message'] = '服务器错误！';
        }
      );
  }

  register(user:User):void {
    this.http
      .get(constant.BUSSINESS_SERVER_URL+'/rest/users/register?username='+user.username+'&password='+user.password)
      .subscribe(
        data => {
          if(data['success'] == true){
            this.router.navigate(['/login']);
          }
          this.data = data;
        },
        err => {
          console.log('Something went wrong!');
          this.data['success'] = false;
          this.data['message'] = '服务器错误！';
        }
      );
  }

  logout():void{
    this.user = new User;
    this.router.navigate(['/login']);
  }

}
