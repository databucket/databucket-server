import {useState, useEffect} from 'react';

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