import React from 'react';
import { createMuiTheme, ThemeProvider } from '@material-ui/core/styles';
import Cookies from 'universal-cookie';
import DatabucketMainDrawer from './components/DatabucketMainDrower';
// import ConditionsTable from './components/conditionsTable/ConditionsTable';

export default function App() {

  // window.API = 'http://localhost:8080/api';
  window.API = './api';

  // set user name
  const cookies = new Cookies();
  const userName = cookies.get('user_name');

  if (userName == null) {
    let newUserName = prompt('Type your nick or name (max 20 characters).\n\nIt is required to create/modify/delete items.\n\n');
    if (newUserName == null)
      newUserName = 'unknown';

    if (newUserName.length > 20)
      newUserName = newUserName.substr(0, 20);

    const current = new Date();
    const nextYear = new Date();
    nextYear.setFullYear(current.getFullYear() + 1);
    cookies.set('user_name', newUserName, { path: '/', expires: nextYear });
    window.USER = newUserName;
  } else {
    window.USER = userName;
  }

  const theme = createMuiTheme({
    palette: {
      primary: {
        // main: '#353a47',
        // main: '#495677', // main databucket logo color
        // main: '#6d7792',
        // main: '#3e4864'
        main: '#0d47a1', // granatowy
      },
      // secondary: {
      //   main: '#f44336',
      // },
    },
  });

  return (
    <ThemeProvider theme={theme}>
      <DatabucketMainDrawer />
      {/* <ConditionsTable /> */}
    </ThemeProvider>
  );
}
