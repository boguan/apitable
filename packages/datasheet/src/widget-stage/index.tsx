import * as components from '@apitable/components';
import * as core from '@apitable/core';
import * as icons from '@apitable/icons';
import * as widgetSdk from '@apitable/widget-sdk';
import 'focus-options-polyfill';
import 'get-root-node-polyfill/implement';
import 'normalize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import 'regenerator-runtime/runtime';
import 'resize-observer-polyfill/dist/ResizeObserver.global';
import { Main } from './main/main';
import { initTheme } from './theme';
import { ThemeWrap } from './theme_wrap';

(() => {
  if (!process.env.SSR) {
    window['_React'] = React;
    window['_ReactDom'] = ReactDOM;
    window['_@vikadata/components'] = components;
    window['_@vikadata/widget-sdk'] = widgetSdk;
    window['_@vikadata/core'] = core;
    window['_@vikadata/icons'] = icons;
  }
})();

initTheme();

const WidgetStage = () => {
  return <ThemeWrap>
    <Main />
  </ThemeWrap>;
};

export default WidgetStage;
