import { MoviePage } from './app.po';

describe('movie App', () => {
  let page: MoviePage;

  beforeEach(() => {
    page = new MoviePage();
  });

  it('should display welcome message', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Welcome to app!');
  });
});
