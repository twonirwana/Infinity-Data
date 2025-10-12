# Infinity Data

A program to convert unit data from Corvus Belli's Army Builder for the Infinity tabletop game.

Currently, the main function is to take an army code and generate nice, printable cards for all the units in the army.

If Corvus Belli provides an image for the unit profile, it is included in the card.

The default implementation creates the cards in the size of one-third of an A4 page, so 9 cards fit on one page for printing.

The cards look like this:

![example.png](example.png)

[kestrel army.pdf](kestrel.pdf)

## Usage

Currently, you need to execute the application in an IDE, or you can download the JAR file, install Java 21, and execute it with the following terminal command:


``java -jar Infinity-Data-0.1-SNAPSHOT-all.jar axZrZXN0cmVsLWNvbG9uaWFsLWZvcmNlASCBLAIBAQAKAIcMAQYAAIcNAQMAAIcWAQEAAIcLAQEAAIcLAQoAAA8BCgAAhxUBAQAAhxUBAgAADgEBAACHDwEBAAIBAAUAhxABBAAAhxEBAwAAhxIBAwAAJQEBAACHFAEBAA%3D%3D``

Replace the army code with your own, and the program will create an `out\html\card` directory with an HTML file containing the cards.

The HTML file can then be opened in a browser and printed or exported to PDF.

Optionally, the parameter `false` (with a space) can be added after the army code to generate the cards in centimeters instead of inches.

## Development

Contributions of any kind are welcome, especially if someone has experience in HTML layout and wants to help make the cards look nicer.

Based on [Infinity-Army-Tools](https://github.com/cwoac/Infinity-Army-Tools)