import {useEffect, useState} from 'react';

export function useWindowDimension() {
    const [dimension, setDimension] = useState([
        window.innerHeight,
        window.innerWidth,
    ]);
    useEffect(() => {
        const debouncedResizeHandler = debounce(() => {
            setDimension([window.innerHeight, window.innerWidth,]);
        }, 200);
        window.addEventListener('resize', debouncedResizeHandler);
        return () => window.removeEventListener('resize', debouncedResizeHandler);
    }, []);
    return dimension;
}

function debounce(fn, ms) {
    let timer;
    return _ => {
        clearTimeout(timer);
        timer = setTimeout(_ => {
            timer = null;
            fn.apply(this, arguments);
        }, ms);
    };
}

export const debounce2 = (func, wait, immediate) => {
    let timeout;

    return (...args) => {
        let context = this;
        let later = () => {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };

        let callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
}
