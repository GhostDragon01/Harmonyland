import React from "react";
import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';

import AlertPage from "./pages/AlertPage";
import Analytics from "./pages/Analytics";
import './App.css';

function App() {
    return (
        <Router>
            <Routes>
                <Route path='/' element={<AlertPage />} />
                <Route path='/analytics' element={<Analytics />} />
            </Routes>

        </Router>
    );
}

export default App;