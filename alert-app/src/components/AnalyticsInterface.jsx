import React, { useEffect, useState } from "react";

import BarChart from "./BartChart";
import PieChart from "./PieChart";
import './css/Analytics.css';
import TitleApp from "./TitleApp";

const AnalyticsInterface = () => {
    const [data, setData] = useState(null);

    useEffect(() => {
        fetchData(); // Fetch data immediately when the component mounts

        // Fetch data every 30 seconds
        const timer = setInterval(fetchData, 30000);

        // Clean up the timer when the component unmounts
        return () => {
            clearInterval(timer);
        };
    }, []);

    const fetchData = async () => {
        try {
            const response = await fetch('http://localhost:8081/data');
            const json = await response.json();
            setData(json);
            console.log('Server Response:', json);
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };

    // Define the state variables inside the component function
    const [ageGroups, setAgeGroups] = useState(null);
    const [topWords, setTopWords] = useState(null);
    const [nightRiot, setNightRiot] = useState(null);

    // Update the state variables when the data state changes
    useEffect(() => {
        if (data) {
            const ageGroupData = {
                labels: data.ageGroupPercentages.map(([ageGroup, _]) => ageGroup),
                datasets: [
                    {
                        label: "Alerts",
                        data: data.ageGroupPercentages.map(([_, percentage]) => percentage),
                        backgroundColor: [
                            "rgba(75,192,192,1)",
                            "#ecf0f1",
                            "#50AF95",
                            "#f3ba2f",
                            "#2a71d0",
                        ],
                        borderColor: "black",
                        borderWidth: 2,
                    },
                ],
            };
            setAgeGroups(ageGroupData);

            const topWordsData = {
                labels: data.topWords.map(([word, _]) => word),
                datasets: [
                    {
                        label: "Alerts",
                        data: data.topWords.map(([_, count]) => count),
                        backgroundColor: [
                            "rgba(75,192,192,1)",
                            "#ecf0f1",
                            "#50AF95",
                            "#f3ba2f",
                            "#2a71d0",
                        ],
                        borderColor: "black",
                        borderWidth: 2,
                    },
                ],
            };
            setTopWords(topWordsData);

            const nightRiotData = {
                labels: ['Night Riots', 'Day Riots'],
                datasets: [
                    {
                        label: "Alerts",
                        data: [data.ratioOfNightRiot, 100 - data.ratioOfNightRiot],
                        backgroundColor: [
                            "rgba(75,192,192,1)",
                            "#ecf0f1",
                            "#50AF95",
                            "#f3ba2f",
                            "#2a71d0",
                        ],
                        borderColor: "black",
                        borderWidth: 2,
                        color: "black",
                    },
                ],
            };
            setNightRiot(nightRiotData);
        }
    }, [data]);

    return (
        <div>
            <TitleApp message={"Show Alerts"} link={"/"}/>
            <div className="container">
                {data ? (
                    <>
                        <div className="graph-container-bar">
                            <h2 className="graph-title-bar">Age groups</h2>
                            <div className="chart-bar">
                                {ageGroups && <BarChart chartData={ageGroups} />}
                            </div>
                        </div>
                        <div className="graph-container">
                            <h2 className="graph-title">Top 3 words</h2>
                            <div className="chart">
                                {topWords && <PieChart chartData={topWords} />}
                            </div>
                        </div>
                        <div className="graph-container">
                            <h2 className="graph-title">Day/Night alerts</h2>
                            <div className="chart">
                                {nightRiot && <PieChart chartData={nightRiot} />}
                            </div>
                        </div>
                        <div className="average-alert">
                            <h2>Average alerts per report</h2>
                            <p> <strong>{data.alertAverage.toFixed(1)}</strong></p>
                        </div>
                    </>
                ) : (
                    <p>Loading data...</p>
                )}
            </div>
        </div>
    )
}

export default AnalyticsInterface;
