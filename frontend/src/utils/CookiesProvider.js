// import Cookies from "universal-cookie/es6";
//
// const THEME_NAME = "theme-name";
//
// export function saveThemeName(name) {
//     let cookies = new Cookies();
//     const current = new Date();
//     const nextYear = new Date();
//     nextYear.setFullYear(current.getFullYear() + 1);
//     cookies.set(THEME_NAME, name, {path: '/', expires: nextYear});
// }
//
// export function getThemeName() {
//     let cookies = new Cookies();
//     const themeName = cookies.get(THEME_NAME);
//     return themeName != null ? themeName : 'light';
// }